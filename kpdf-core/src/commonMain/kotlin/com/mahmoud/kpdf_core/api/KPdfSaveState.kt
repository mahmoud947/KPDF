package com.mahmoud.kpdf_core.api

/**
 * Observable save/export lifecycle state for a viewer instance.
 */
sealed interface KPdfSaveState {
    data object Idle : KPdfSaveState

    data object Exporting : KPdfSaveState

    data class AwaitingDestination(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
    ) : KPdfSaveState

    data class Success(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
        val location: String? = null,
    ) : KPdfSaveState

    data class Cancelled(
        val requestId: Long,
        val suggestedFileName: String,
        val mimeType: String,
    ) : KPdfSaveState

    data class Error(
        val reason: KPdfError,
        val suggestedFileName: String? = null,
        val mimeType: String? = null,
    ) : KPdfSaveState
}
