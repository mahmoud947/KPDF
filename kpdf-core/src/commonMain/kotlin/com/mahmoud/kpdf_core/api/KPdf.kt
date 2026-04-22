package com.mahmoud.kpdf_core.api

/**
 * Facade entry point for the PDF SDK.
 */
interface KPdf {

    /**
     * Creates a viewer state-holder for [source].
     */
    fun viewerState(
        source: KPdfSource,
        config: KPdfViewerConfig = KPdfViewerConfig.builder().build(),
    ): KPdf
}
