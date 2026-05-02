package com.mahmoud.kpdf_core.viewer

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfExternalOpenRequest
import com.mahmoud.kpdf_core.api.KPdfExternalOpenResult
import com.mahmoud.kpdf_core.api.KPdfExternalOpenState
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfLoadState
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentRequest
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentResult
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentState
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import com.mahmoud.kpdf_core.api.KPdfSaveRequest
import com.mahmoud.kpdf_core.api.KPdfSaveResult
import com.mahmoud.kpdf_core.api.KPdfSaveState
import com.mahmoud.kpdf_core.api.KPdfSearchState
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import com.mahmoud.kpdf_core.cache.KPdfMemoryPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCache
import com.mahmoud.kpdf_core.cache.KPdfPageCacheKey
import com.mahmoud.kpdf_core.cache.cacheKey
import com.mahmoud.kpdf_core.error.toKPdfError
import com.mahmoud.kpdf_core.repository.KPdfRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _openDocumentState = MutableStateFlow<KPdfOpenDocumentState>(KPdfOpenDocumentState.Idle)
    private val _openDocumentRequests = MutableSharedFlow<KPdfOpenDocumentRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _saveState = MutableStateFlow<KPdfSaveState>(KPdfSaveState.Idle)
    private val _saveRequests = MutableSharedFlow<KPdfSaveRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _externalOpenState = MutableStateFlow<KPdfExternalOpenState>(KPdfExternalOpenState.Idle)
    private val _externalOpenRequests = MutableSharedFlow<KPdfExternalOpenRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _currentPage = MutableStateFlow(0)
    private val _currentZoom = MutableStateFlow(config.minZoom)
    private val _searchState = MutableStateFlow<KPdfSearchState>(KPdfSearchState.Idle)
    private val inFlightRenderMutex = Mutex()
    private val inFlightRenders = mutableMapOf<KPdfPageCacheKey, Deferred<Result<KPdfPageBitmap>>>()

    private var document: KPdfDocumentRef? = null
    private var documentCacheKey: String? = null
    private var openJob: Job? = null
    private var renderJob: Job? = null
    private var preloadJob: Job? = null
    private var searchJob: Job? = null
    private var saveJob: Job? = null
    private var externalOpenJob: Job? = null
    private var renderGeneration = 0
    private var nextOpenDocumentRequestId = 0L
    private var nextSaveRequestId = 0L
    private var nextExternalOpenRequestId = 0L
    private var activeOpenDocumentRequest: KPdfOpenDocumentRequest? = null
    private var activeSaveRequest: KPdfSaveRequest? = null
    private var activeExternalOpenRequest: KPdfExternalOpenRequest? = null

    override val loadState: StateFlow<KPdfLoadState> = _loadState
    override val currentPageIndex: StateFlow<Int> = _currentPage
    override val currentZoom: StateFlow<Float> = _currentZoom
    override val searchState: StateFlow<KPdfSearchState> = _searchState
    override val renderedPage: StateFlow<KPdfRenderedPageState> = _renderedPage
    override val openDocumentState: StateFlow<KPdfOpenDocumentState> = _openDocumentState
    override val openDocumentRequests: Flow<KPdfOpenDocumentRequest> = _openDocumentRequests.asSharedFlow()
    override val saveState: StateFlow<KPdfSaveState> = _saveState
    override val saveRequests: Flow<KPdfSaveRequest> = _saveRequests.asSharedFlow()
    override val externalOpenState: StateFlow<KPdfExternalOpenState> = _externalOpenState
    override val externalOpenRequests: Flow<KPdfExternalOpenRequest> = _externalOpenRequests.asSharedFlow()

    override fun open(source: KPdfSource) {
        this.source = source
        openJob?.cancel()
        renderJob?.cancel()
        preloadJob?.cancel()
        searchJob?.cancel()
        resetOpenDocumentFlow()
        resetSaveFlow()
        resetExternalOpenFlow()
        _searchState.update { KPdfSearchState.Idle }

        openJob = scope.launch {
            closeCurrentDocument()

            _loadState.update { KPdfLoadState.Loading }
            _renderedPage.update { KPdfRenderedPageState.Idle }
            _currentPage.update { 0 }
            _currentZoom.update { config.minZoom }

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
                        val initialPageIndex = _currentPage.value.coerceIn(0, opened.pageCount - 1)
                        _currentPage.update { initialPageIndex }
                        renderCurrentPage(pageIndex = initialPageIndex)
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
            _currentZoom.update { config.minZoom }
            renderCurrentPage(pageIndex = index)
        }
    }

    override fun setZoom(zoom: Float) {
        val targetZoom = zoom.coerceIn(config.minZoom, config.maxZoom)
        _currentZoom.update { targetZoom }
    }

    override fun zoomIn() {
        setZoom(_currentZoom.value + ZoomStep)
    }

    override fun zoomOut() {
        setZoom(_currentZoom.value - ZoomStep)
    }

    override fun resetZoom() {
        _currentZoom.update { config.minZoom }
    }

    override fun searchText(query: String) {
        val normalizedQuery = query.trim()
        val currentDocument = document

        searchJob?.cancel()
        if (normalizedQuery.isEmpty()) {
            _searchState.update { KPdfSearchState.Idle }
            return
        }

        if (currentDocument == null) {
            _searchState.update {
                KPdfSearchState.Error(
                    query = normalizedQuery,
                    reason = KPdfError.Unknown("No PDF document is open."),
                )
            }
            return
        }

        searchJob = scope.launch {
            _searchState.update { KPdfSearchState.Searching(normalizedQuery) }

            currentDocument.searchText(normalizedQuery).fold(
                onSuccess = { results ->
                    val success = KPdfSearchState.Success(
                        query = normalizedQuery,
                        results = results,
                    )
                    _searchState.update { success }
                    success.activeResult?.let { result ->
                        goToPage(result.pageIndex)
                    }
                },
                onFailure = { throwable ->
                    _searchState.update {
                        KPdfSearchState.Error(
                            query = normalizedQuery,
                            reason = throwable.toKPdfError(),
                        )
                    }
                },
            )
        }
    }

    override fun nextSearchResult() {
        moveSearchResultBy(1)
    }

    override fun previousSearchResult() {
        moveSearchResultBy(-1)
    }

    override fun clearSearch() {
        searchJob?.cancel()
        searchJob = null
        _searchState.update { KPdfSearchState.Idle }
    }

    override fun close() {
        openJob?.cancel()
        renderJob?.cancel()
        preloadJob?.cancel()
        searchJob?.cancel()
        resetOpenDocumentFlow()
        resetSaveFlow()
        resetExternalOpenFlow()
        _searchState.update { KPdfSearchState.Idle }

        scope.launch {
            closeCurrentDocument()
            _currentPage.update { 0 }
            _currentZoom.update { config.minZoom }
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

    override suspend fun exportPdf(): Result<ByteArray> =
        repository.export(source)

    override fun requestOpenFromDevice(
        mimeTypes: List<String>,
    ) {
        if (activeOpenDocumentRequest != null) return

        val normalizedMimeTypes = mimeTypes
            .map(String::trim)
            .filter(String::isNotEmpty)
            .ifEmpty { listOf(KPdfOpenDocumentRequest.PdfMimeType) }
        val request = KPdfOpenDocumentRequest(
            requestId = ++nextOpenDocumentRequestId,
            mimeTypes = normalizedMimeTypes,
        )

        activeOpenDocumentRequest = request
        _openDocumentState.update {
            KPdfOpenDocumentState.AwaitingSelection(
                requestId = request.requestId,
                mimeTypes = request.mimeTypes,
            )
        }
        _openDocumentRequests.tryEmit(request)
    }

    override fun onOpenFromDeviceResult(
        requestId: Long,
        result: KPdfOpenDocumentResult,
    ) {
        val request = activeOpenDocumentRequest
            ?.takeIf { it.requestId == requestId }
            ?: return

        activeOpenDocumentRequest = null
        when (result) {
            is KPdfOpenDocumentResult.Success -> {
                _openDocumentState.update {
                    KPdfOpenDocumentState.Success(result.source)
                }
            }

            is KPdfOpenDocumentResult.Failure -> {
                _openDocumentState.update {
                    KPdfOpenDocumentState.Error(result.reason)
                }
            }

            KPdfOpenDocumentResult.Cancelled -> {
                _openDocumentState.update { KPdfOpenDocumentState.Cancelled }
            }
        }
    }

    override fun requestSave(
        suggestedFileName: String?,
        mimeType: String,
    ) {
        if (_saveState.value is KPdfSaveState.Exporting || activeSaveRequest != null) {
            return
        }

        val normalizedMimeType = mimeType.ifBlank { KPdfSaveRequest.DefaultMimeType }
        val resolvedFileName = resolveSuggestedFileName(
            suggestedFileName = suggestedFileName,
            mimeType = normalizedMimeType,
        )
        val requestId = ++nextSaveRequestId

        saveJob?.cancel()
        saveJob = scope.launch {
            _saveState.update { KPdfSaveState.Exporting }

            try {
                exportPdf().fold(
                    onSuccess = { bytes ->
                        val request = KPdfSaveRequest(
                            requestId = requestId,
                            suggestedFileName = resolvedFileName,
                            mimeType = normalizedMimeType,
                            bytes = bytes,
                        )
                        activeSaveRequest = request
                        _saveState.update {
                            KPdfSaveState.AwaitingDestination(
                                requestId = request.requestId,
                                suggestedFileName = request.suggestedFileName,
                                mimeType = request.mimeType,
                            )
                        }
                        _saveRequests.emit(request)
                    },
                    onFailure = { throwable ->
                        _saveState.update {
                            KPdfSaveState.Error(
                                reason = throwable.toKPdfError(),
                                suggestedFileName = resolvedFileName,
                                mimeType = normalizedMimeType,
                            )
                        }
                    },
                )
            } catch (_: CancellationException) {
                _saveState.update { KPdfSaveState.Idle }
            }
        }
    }

    override fun onSaveResult(
        requestId: Long,
        result: KPdfSaveResult,
    ) {
        val request = activeSaveRequest
            ?.takeIf { it.requestId == requestId }
            ?: return

        activeSaveRequest = null
        _saveState.update {
            when (result) {
                is KPdfSaveResult.Success -> {
                    KPdfSaveState.Success(
                        requestId = request.requestId,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                        location = result.location,
                    )
                }

                is KPdfSaveResult.Failure -> {
                    KPdfSaveState.Error(
                        reason = result.reason,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                    )
                }

                KPdfSaveResult.Cancelled -> {
                    KPdfSaveState.Cancelled(
                        requestId = request.requestId,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                    )
                }
            }
        }
    }

    override fun requestOpenInExternalApp(
        suggestedFileName: String?,
        mimeType: String,
    ) {
        if (_externalOpenState.value is KPdfExternalOpenState.Exporting || activeExternalOpenRequest != null) {
            return
        }

        val normalizedMimeType = mimeType.ifBlank { KPdfExternalOpenRequest.DefaultMimeType }
        val resolvedFileName = resolveSuggestedFileName(
            suggestedFileName = suggestedFileName,
            mimeType = normalizedMimeType,
        )
        val requestId = ++nextExternalOpenRequestId

        externalOpenJob?.cancel()
        externalOpenJob = scope.launch {
            _externalOpenState.update { KPdfExternalOpenState.Exporting }

            try {
                exportPdf().fold(
                    onSuccess = { bytes ->
                        val request = KPdfExternalOpenRequest(
                            requestId = requestId,
                            suggestedFileName = resolvedFileName,
                            mimeType = normalizedMimeType,
                            bytes = bytes,
                        )
                        activeExternalOpenRequest = request
                        _externalOpenState.update {
                            KPdfExternalOpenState.AwaitingExternalApp(
                                requestId = request.requestId,
                                suggestedFileName = request.suggestedFileName,
                                mimeType = request.mimeType,
                            )
                        }
                        _externalOpenRequests.emit(request)
                    },
                    onFailure = { throwable ->
                        _externalOpenState.update {
                            KPdfExternalOpenState.Error(
                                reason = throwable.toKPdfError(),
                                suggestedFileName = resolvedFileName,
                                mimeType = normalizedMimeType,
                            )
                        }
                    },
                )
            } catch (_: CancellationException) {
                _externalOpenState.update { KPdfExternalOpenState.Idle }
            }
        }
    }

    override fun onExternalOpenResult(
        requestId: Long,
        result: KPdfExternalOpenResult,
    ) {
        val request = activeExternalOpenRequest
            ?.takeIf { it.requestId == requestId }
            ?: return

        activeExternalOpenRequest = null
        _externalOpenState.update {
            when (result) {
                is KPdfExternalOpenResult.Success -> {
                    KPdfExternalOpenState.Success(
                        requestId = request.requestId,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                        location = result.location,
                    )
                }

                is KPdfExternalOpenResult.Failure -> {
                    KPdfExternalOpenState.Error(
                        reason = result.reason,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                    )
                }

                KPdfExternalOpenResult.Cancelled -> {
                    KPdfExternalOpenState.Cancelled(
                        requestId = request.requestId,
                        suggestedFileName = request.suggestedFileName,
                        mimeType = request.mimeType,
                    )
                }
            }
        }
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

    private fun resetOpenDocumentFlow() {
        activeOpenDocumentRequest = null
        _openDocumentState.update { KPdfOpenDocumentState.Idle }
    }

    private fun resetSaveFlow() {
        saveJob?.cancel()
        saveJob = null
        activeSaveRequest = null
        _saveState.update { KPdfSaveState.Idle }
    }

    private fun resetExternalOpenFlow() {
        externalOpenJob?.cancel()
        externalOpenJob = null
        activeExternalOpenRequest = null
        _externalOpenState.update { KPdfExternalOpenState.Idle }
    }

    private fun moveSearchResultBy(delta: Int) {
        val current = _searchState.value as? KPdfSearchState.Success ?: return
        if (current.results.isEmpty()) return

        val nextIndex = (current.activeResultIndex + delta).floorMod(current.results.size)
        val nextState = current.copy(activeResultIndex = nextIndex)
        _searchState.update { nextState }
        nextState.activeResult?.let { result ->
            goToPage(result.pageIndex)
        }
    }

    private fun resolveSuggestedFileName(
        suggestedFileName: String?,
        mimeType: String,
    ): String {
        val explicitName = suggestedFileName?.trim().orEmpty()
        if (explicitName.isNotEmpty()) {
            return explicitName
        }

        val documentTitle = (loadState.value as? KPdfLoadState.Ready)
            ?.title
            ?.trim()
            .orEmpty()

        if (documentTitle.isNotEmpty()) {
            return documentTitle
        }

        return defaultFileNameForMimeType(mimeType)
    }

    private fun defaultFileNameForMimeType(mimeType: String): String =
        when (mimeType.lowercase()) {
            KPdfSaveRequest.DefaultMimeType -> "document.pdf"
            else -> "document"
        }

    private companion object {
        const val DefaultTargetWidth = 1200
        const val DefaultTargetHeight = 1600
        const val ZoomStep = 0.25f
    }
}

private fun Int.floorMod(other: Int): Int =
    ((this % other) + other) % other

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
