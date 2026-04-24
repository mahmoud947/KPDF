package com.mahmoud.kpdf_core.api

/**
 * Result returned from platform document picker integrations.
 */
sealed interface KPdfOpenDocumentResult {
    data class Success(
        val source: KPdfSource,
    ) : KPdfOpenDocumentResult

    data class Failure(
        val reason: KPdfError,
    ) : KPdfOpenDocumentResult

    data object Cancelled : KPdfOpenDocumentResult
}
