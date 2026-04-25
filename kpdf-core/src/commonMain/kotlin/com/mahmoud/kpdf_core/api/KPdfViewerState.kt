package com.mahmoud.kpdf_core.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * State-holder and controller for a single PDF viewer instance.
 *
 * The state exposes unidirectional observable values through [StateFlow] and
 * keeps document loading, rendering, and source details behind the SDK boundary.
 */
 interface KPdfViewerState {
     val source: KPdfSource
     val config: KPdfViewerConfig
     val loadState: StateFlow<KPdfLoadState>
     val currentPageIndex: StateFlow<Int>
     val currentZoom: StateFlow<Float>

     val renderedPage: StateFlow<KPdfRenderedPageState>
     val openDocumentState: StateFlow<KPdfOpenDocumentState>
     val openDocumentRequests: Flow<KPdfOpenDocumentRequest>
     val saveState: StateFlow<KPdfSaveState>
     val saveRequests: Flow<KPdfSaveRequest>

    /**
     * Opens [source], cancelling any in-flight open/render work owned by this
     * state instance.
     */
    public fun open(source: KPdfSource = this.source)

    /**
     * Retries the most recent [open] request.
     */
    public fun retry()


    /**
     * Navigates to and renders the next page of the document.
     */
     fun nextPage()

    /**
     * Navigates to the page immediately preceding the currently rendered page.
     */
     fun previousPage()

    /**
     * Navigates the viewer to the specified [pageIndex].
     *
     * This updates the state to trigger the rendering of the requested page.
     *
     * @param pageIndex The 0-based index of the page to display.
     */
     fun goToPage(index: Int)

    /**
     * Updates the current zoom level for the active page surface.
     */
     fun setZoom(zoom: Float)

    /**
     * Increases the current zoom level by one viewer-defined step.
     */
     fun zoomIn()

    /**
     * Decreases the current zoom level by one viewer-defined step.
     */
     fun zoomOut()

    /**
     * Resets the current zoom back to the configured minimum.
     */
     fun resetZoom()

    /**
     * Closes the active document and releases resources owned by this state.
     */
    public fun close()

    /**
     * Requests a local-device PDF picker from compose integrations.
     */
    public fun requestOpenFromDevice(
        mimeTypes: List<String> = listOf(KPdfOpenDocumentRequest.PdfMimeType),
    )

    /**
     * Receives the platform picker result for the most recent open request.
     */
    public fun onOpenFromDeviceResult(
        requestId: Long,
        result: KPdfOpenDocumentResult,
    )

    /**
     * Requests that the current PDF be exported and saved through compose
     * platform integrations.
     */
    public fun requestSave(
        suggestedFileName: String? = null,
        mimeType: String = KPdfSaveRequest.DefaultMimeType,
    )

    /**
     * Alias for [requestSave] to keep UI call sites concise.
     */
    public fun savePdf(
        suggestedFileName: String? = null,
        mimeType: String = KPdfSaveRequest.DefaultMimeType,
    ) {
        requestSave(
            suggestedFileName = suggestedFileName,
            mimeType = mimeType,
        )
    }

    /**
     * Receives the platform save result for the most recent request.
     */
    public fun onSaveResult(
        requestId: Long,
        result: KPdfSaveResult,
    )

    /**
     * Renders [pageIndex] for UI layers without exposing source or platform
     * renderer details.
     */
    public suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float = 1f,
    ): Result<KPdfPageBitmap>

    /**
     * Exports the currently configured PDF source as raw bytes.
     */
    public suspend fun exportPdf(): Result<ByteArray>
}
