package com.mahmoud.kpdf_core.api

/**
 * One-shot local document picker request emitted by [KPdfViewerState].
 */
data class KPdfOpenDocumentRequest(
    val requestId: Long,
    val mimeTypes: List<String>,
) {
    companion object {
        const val PdfMimeType: String = "application/pdf"
    }
}
