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
import com.mahmoud.kpdf_core.cache.KPdfMemoryPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCacheKey
import com.mahmoud.kpdf_core.error.toKPdfError
import com.mahmoud.kpdf_core.repository.KPdfRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Real shared viewer state-holder and controller.
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

class DefaultKPdfViewerState(
    override var source: KPdfSource,
    override val config: KPdfViewerConfig,
    private val repository: KPdfRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : KPdfViewerState {

    private val _loadState = MutableStateFlow<KPdfLoadState>(KPdfLoadState.Idle)
    private val _renderedPage = MutableStateFlow<KPdfRenderedPageState>(KPdfRenderedPageState.Idle)
    private val _currentPage = MutableStateFlow(0)
    private val pageCache: KPdfPageCache = KPdfMemoryPageCache(config.ramCacheSize)

    private var document: KPdfDocumentRef? = null
    private var openJob: Job? = null
    private var renderJob: Job? = null
    private var renderGeneration = 0

    override val loadState: StateFlow<KPdfLoadState> = _loadState
    override val renderedPage: StateFlow<KPdfRenderedPageState> = _renderedPage

    override fun open(source: KPdfSource) {
        this.source = source
        openJob?.cancel()
        renderJob?.cancel()

        openJob = scope.launch {
            closeCurrentDocument()

            _loadState.update { KPdfLoadState.Loading }
            _renderedPage.update { KPdfRenderedPageState.Idle }

            repository.open(source).fold(
                onSuccess = { opened ->
                    document = opened
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

        if (pageIndex !in 0 until currentDocument.pageCount) {
            return Result.failure(
                KPdfException(KPdfError.Unknown("Page index out of bounds: $pageIndex"))
            )
        }

        return renderPageWithCache(
            currentDocument = currentDocument,
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom,
        )
    }

    private fun renderCurrentPage(pageIndex: Int) {
        val currentDocument = document ?: return
        if (pageIndex !in 0 until currentDocument.pageCount) return

        val generation = ++renderGeneration
        renderJob?.cancel()

        renderJob = scope.launch {
            _renderedPage.update { KPdfRenderedPageState.Loading }

            renderPageWithCache(
                currentDocument = currentDocument,
                pageIndex = pageIndex,
                targetWidth = DefaultTargetWidth,
                targetHeight = DefaultTargetHeight,
                zoom = 1f,
            ).fold(
                onSuccess = { bitmap ->
                    if (generation == renderGeneration) {
                        _renderedPage.update { KPdfRenderedPageState.Ready(bitmap) }
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

    private suspend fun renderPageWithCache(
        currentDocument: KPdfDocumentRef,
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> {
        val cacheKey = KPdfPageCacheKey(
            documentId = currentDocument.id,
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom
        )

        pageCache.get(cacheKey)?.let { cachedPage ->
            return Result.success(cachedPage)
        }

        val renderResult = currentDocument.renderPage(
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = zoom
        )

        renderResult.getOrNull()?.let { renderedPage ->
            pageCache.put(cacheKey, renderedPage)
        }

        return renderResult
    }

    private suspend fun closeCurrentDocument() {
        val currentDocument = document ?: return
        document = null
        pageCache.removeDocument(currentDocument.id)
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
