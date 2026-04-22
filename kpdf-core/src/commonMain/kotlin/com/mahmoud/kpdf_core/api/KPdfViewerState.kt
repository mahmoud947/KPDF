package com.mahmoud.kpdf_core.api

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

     val renderedPage: StateFlow<KPdfRenderedPageState>

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
     * Closes the active document and releases resources owned by this state.
     */
    public fun close()

    /**
     * Renders [pageIndex] for UI layers without exposing source or platform
     * renderer details.
     */
    public suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Result<KPdfPageBitmap>
}
