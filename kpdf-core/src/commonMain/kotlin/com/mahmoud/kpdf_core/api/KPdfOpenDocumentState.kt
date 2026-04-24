package com.mahmoud.kpdf_core.api

/**
 * Observable state for the local-device document picker flow.
 */
sealed interface KPdfOpenDocumentState {
    data object Idle : KPdfOpenDocumentState

    data class AwaitingSelection(
        val requestId: Long,
        val mimeTypes: List<String>,
    ) : KPdfOpenDocumentState

    data object Cancelled : KPdfOpenDocumentState

    data class Error(
        val reason: KPdfError,
    ) : KPdfOpenDocumentState
}
