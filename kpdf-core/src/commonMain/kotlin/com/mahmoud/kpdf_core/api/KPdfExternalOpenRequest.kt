package com.mahmoud.kpdf_core.api

/**
 * One-shot request emitted by [KPdfViewerState] to open the current PDF in an
 * external PDF-capable application.
 */
data class KPdfExternalOpenRequest(
    val requestId: Long,
    val suggestedFileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    companion object {
        const val DefaultMimeType: String = "application/pdf"
    }
}
