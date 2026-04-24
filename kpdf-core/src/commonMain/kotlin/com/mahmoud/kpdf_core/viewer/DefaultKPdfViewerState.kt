package com.mahmoud.kpdf_core.viewer

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfLoadState
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import com.mahmoud.kpdf_core.api.cacheKey
import com.mahmoud.kpdf_core.cache.KPdfMemoryPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCacheKey
import com.mahmoud.kpdf_core.error.toKPdfError
import com.mahmoud.kpdf_core.repository.KPdfRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real shared viewer state-holder and controller.
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class DefaultKPdfViewerState(
    override var source: KPdfSource,
    override val config: KPdfViewerConfig,
    private val repository: KPdfRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val pageCache: KPdfPageCache = KPdfMemoryPageCache(config.ramCacheSize),
) : KPdfViewerState {

    private val _loadState = MutableStateFlow<KPdfLoadState>(KPdfLoadState.Idle)
    private val _renderedPage = MutableStateFlow<KPdfRenderedPageState>(KPdfRenderedPageState.Idle)
    private val _currentPage = MutableStateFlow(0)
    private val inFlightRenderMutex = Mutex()
    private val inFlightRenders = mutableMapOf<KPdfPageCacheKey, Deferred<Result<KPdfPageBitmap>>>()

    private var document: KPdfDocumentRef? = null
    private var documentCacheKey: String? = null
    private var openJob: Job? = null
    private var renderJob: Job? = null
    private var preloadJob: Job? = null
    private var renderGeneration = 0

    override val loadState: StateFlow<KPdfLoadState> = _loadState
    override val renderedPage: StateFlow<KPdfRenderedPageState> = _renderedPage

    override fun open(source: KPdfSource) {
        this.source = source
        openJob?.cancel()
        renderJob?.cancel()
        preloadJob?.cancel()

        openJob = scope.launch {
            closeCurrentDocument()

            _loadState.update { KPdfLoadState.Loading }
            _renderedPage.update { KPdfRenderedPageState.Idle }

            repository.open(source).fold(
                onSuccess = { opened ->
                    document = opened
                    documentCacheKey = source.cacheKey()
                    _loadState.update {
                        KPdfLoadState.Ready(
                            pageCount = opened.pageCount,
                            title = opened.title,
                        )
                    }

                    if (opened.pageCount > 0) {
                        renderCurrentPage(pageIndex = _currentPage.value)
                    } else {
                        _renderedPage.update {
                            KPdfRenderedPageState.Error(
                                KPdfError.Unknown("PDF document contains no pages.")
                            )
                        }
                    }
                },
                onFailure = { throwable ->
                    documentCacheKey = null
                    val error = throwable.toKPdfError()
                    _loadState.update { KPdfLoadState.Error(error) }
                    _renderedPage.update { KPdfRenderedPageState.Error(error) }
                },
            )
        }
    }

    override fun retry() {
        open(source)
    }

    override fun nextPage() {
        goToPage(_currentPage.value + 1)
    }

    override fun previousPage() {
        goToPage(_currentPage.value - 1)
    }

    override fun goToPage(index: Int) {
        val currentDocument = document ?: return

        if (index !in 0 until currentDocument.pageCount)
            return

        scope.launch {
            _currentPage.update { index }
            renderCurrentPage(pageIndex = index)
        }
    }

    override fun close() {
        openJob?.cancel()
        renderJob?.cancel()
        preloadJob?.cancel()

        scope.launch {
            closeCurrentDocument()
            _loadState.update { KPdfLoadState.Idle }
            _renderedPage.update { KPdfRenderedPageState.Idle }
        }
    }

    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> {
        val currentDocument = document
            ?: return Result.failure(
                KPdfException(KPdfError.Unknown("No PDF document is open."))
            )
        val currentDocumentCacheKey = documentCacheKey
            ?: return Result.failure(
                KPdfException(KPdfError.Unknown("PDF cache key is unavailable."))
            )

        if (pageIndex !in 0 until currentDocument.pageCount) {
            return Result.failure(
                KPdfException(KPdfError.Unknown("Page index out of bounds: $pageIndex"))
            )
        }

        return renderPageWithCache(
            currentDocument = currentDocument,
            currentDocumentCacheKey = currentDocumentCacheKey,
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom,
        )
    }

    private fun renderCurrentPage(pageIndex: Int) {
        val currentDocument = document ?: return
        val currentDocumentCacheKey = documentCacheKey ?: return
        if (pageIndex !in 0 until currentDocument.pageCount) return

        val generation = ++renderGeneration
        renderJob?.cancel()
        preloadJob?.cancel()

        renderJob = scope.launch {
            _renderedPage.update { KPdfRenderedPageState.Loading }

            renderPageWithCache(
                currentDocument = currentDocument,
                currentDocumentCacheKey = currentDocumentCacheKey,
                pageIndex = pageIndex,
                targetWidth = DefaultTargetWidth,
                targetHeight = DefaultTargetHeight,
                zoom = 1f,
            ).fold(
                onSuccess = { bitmap ->
                    if (generation == renderGeneration) {
                        _renderedPage.update { KPdfRenderedPageState.Ready(bitmap) }
                        schedulePagePreload(
                            currentDocument = currentDocument,
                            currentDocumentCacheKey = currentDocumentCacheKey,
                            currentPageIndex = pageIndex,
                            generation = generation,
                        )
                    }
                },
                onFailure = { throwable ->
                    if (generation == renderGeneration) {
                        _renderedPage.update {
                            KPdfRenderedPageState.Error(throwable.toKPdfError())
                        }
                    }
                },
            )
        }
    }

    private fun schedulePagePreload(
        currentDocument: KPdfDocumentRef,
        currentDocumentCacheKey: String,
        currentPageIndex: Int,
        generation: Int,
    ) {
        if (config.preloadPageCount <= 0) return

        preloadJob?.cancel()
        preloadJob = scope.launch {
            preloadPagesAround(
                currentDocument = currentDocument,
                currentDocumentCacheKey = currentDocumentCacheKey,
                currentPageIndex = currentPageIndex,
                generation = generation,
            )
        }
    }

    private suspend fun preloadPagesAround(
        currentDocument: KPdfDocumentRef,
        currentDocumentCacheKey: String,
        currentPageIndex: Int,
        generation: Int,
    ) {
        val pagesToPreload = buildPageLoadSequence(
            currentPageIndex = currentPageIndex,
            lastPageIndex = currentDocument.pageCount - 1,
            preloadPageCount = config.preloadPageCount,
        ).drop(1)

        for (pageIndex in pagesToPreload) {
            if (generation != renderGeneration) return

            renderPageWithCache(
                currentDocument = currentDocument,
                currentDocumentCacheKey = currentDocumentCacheKey,
                pageIndex = pageIndex,
                targetWidth = DefaultTargetWidth,
                targetHeight = DefaultTargetHeight,
                zoom = 1f,
            )
        }
    }

    private suspend fun renderPageWithCache(
        currentDocument: KPdfDocumentRef,
        currentDocumentCacheKey: String,
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> {
        val cacheKey = KPdfPageCacheKey(
            documentKey = currentDocumentCacheKey,
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom
        )

        pageCache.get(cacheKey)?.let { cachedPage ->
            return Result.success(cachedPage)
        }

        return awaitOrStartPageRender(
            cacheKey = cacheKey,
            currentDocument = currentDocument,
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom,
        )
    }

    private suspend fun awaitOrStartPageRender(
        cacheKey: KPdfPageCacheKey,
        currentDocument: KPdfDocumentRef,
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> = coroutineScope {
        while (true) {
            val existingRender = inFlightRenderMutex.withLock {
                inFlightRenders[cacheKey]
            }

            if (existingRender != null) {
                try {
                    return@coroutineScope existingRender.await()
                } catch (_: CancellationException) {
                    inFlightRenderMutex.withLock {
                        if (inFlightRenders[cacheKey] === existingRender) {
                            inFlightRenders.remove(cacheKey)
                        }
                    }

                    pageCache.get(cacheKey)?.let { cachedPage ->
                        return@coroutineScope Result.success(cachedPage)
                    }

                    continue
                }
            }

            val newRender = async(start = CoroutineStart.LAZY) {
                val renderResult = currentDocument.renderPage(
                    pageIndex = pageIndex,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight,
                    zoom = zoom,
                )

                renderResult.getOrNull()?.let { renderedPage ->
                    pageCache.put(cacheKey, renderedPage)
                }

                renderResult
            }

            val renderToAwait = inFlightRenderMutex.withLock {
                inFlightRenders[cacheKey] ?: newRender.also {
                    inFlightRenders[cacheKey] = it
                }
            }

            if (renderToAwait !== newRender) {
                continue
            }

            try {
                newRender.start()
                return@coroutineScope newRender.await()
            } finally {
                inFlightRenderMutex.withLock {
                    if (inFlightRenders[cacheKey] === newRender) {
                        inFlightRenders.remove(cacheKey)
                    }
                }
            }
        }

        error("Page render loop exited unexpectedly.")
    }

    private suspend fun closeCurrentDocument() {
        val currentDocument = document ?: return
        val currentDocumentCacheKey = documentCacheKey
        document = null
        documentCacheKey = null
        currentDocumentCacheKey?.let { pageCache.removeDocument(it) }
        repository.close(currentDocument.id)
    }

    fun dispose() {
        close()
        scope.cancel()
    }

    private companion object {
        const val DefaultTargetWidth = 1200
        const val DefaultTargetHeight = 1600
    }
}

internal fun buildPageLoadSequence(
    currentPageIndex: Int,
    lastPageIndex: Int,
    preloadPageCount: Int,
): List<Int> {
    if (lastPageIndex < 0) return emptyList()

    val boundedCurrentPage = currentPageIndex.coerceIn(0, lastPageIndex)
    val sequence = mutableListOf(boundedCurrentPage)

    for (offset in 1..preloadPageCount.coerceAtLeast(0)) {
        val nextPage = boundedCurrentPage + offset
        if (nextPage <= lastPageIndex) {
            sequence += nextPage
        }

        val previousPage = boundedCurrentPage - offset
        if (previousPage >= 0) {
            sequence += previousPage
        }
    }

    return sequence
}
