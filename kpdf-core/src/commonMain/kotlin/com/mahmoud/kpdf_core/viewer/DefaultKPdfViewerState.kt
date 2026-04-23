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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Real shared viewer state-holder and controller.
 */
class DefaultKPdfViewerState(
    override var source: KPdfSource,
    override val config: KPdfViewerConfig,

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : KPdfViewerState {
    private val _loadState = MutableStateFlow<KPdfLoadState>(KPdfLoadState.Idle)
    private val _renderedPage =
        MutableStateFlow<KPdfRenderedPageState>(KPdfRenderedPageState.Idle)

    private var document: KPdfDocumentRef? = null
    private var openJob: Job? = null
    private var renderJob: Job? = null

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

        }
    }

    override fun retry() {
        open(source)
    }

    override fun close() {
        openJob?.cancel()
        renderJob?.cancel()
        closeCurrentDocument()
        scope.launch {
            _loadState.update { KPdfLoadState.Idle }
            _renderedPage.update { KPdfRenderedPageState.Idle }
        }
    }


    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Result<KPdfPageBitmap> {
        val currentDocument = document
            ?: return Result.failure(KPdfException(KPdfError.Unknown("No PDF document is open.")))
        if (pageIndex !in 0 until currentDocument.pageCount) {
            return Result.failure(KPdfException(KPdfError.Unknown("Page index out of bounds: $pageIndex")))
        }

        val deferred = scope.async {
            currentDocument.renderPage(
                pageIndex = pageIndex,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
            )
        }
        return deferred.await()

    }

    private fun closeCurrentDocument() {
        document ?: return
        document = null
    }

    fun dispose() {
        close()
        scope.cancel()
    }

}
