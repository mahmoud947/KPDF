package com.mahmoud.kpdf_core.api

/**
 * One-shot save request emitted by [KPdfViewerState] for compose integrations.
 */
data class KPdfSaveRequest(
    val requestId: Long,
    val suggestedFileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    companion object {
        const val DefaultMimeType: String = "application/pdf"
    }
}
