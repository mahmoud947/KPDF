package com.mahmoud.kpdf_core.api

/**
 * Observable lifecycle state for opening the current PDF in an external app.
 */
sealed interface KPdfExternalOpenState {
    data object Idle : KPdfExternalOpenState

    data object Exporting : KPdfExternalOpenState

    data class AwaitingExternalApp(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
    ) : KPdfExternalOpenState

    data class Success(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
        val location: String? = null,
    ) : KPdfExternalOpenState

    data class Cancelled(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
    ) : KPdfExternalOpenState

    data class Error(
        val reason: KPdfError,
        val suggestedFileName: String? = null,
        val mimeType: String? = null,
    ) : KPdfExternalOpenState
}
