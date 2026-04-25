package com.mahmoud.kpdf_core.api

/**
 * Result reported back to [KPdfViewerState] after the SDK tries to hand the PDF
 * off to an external app.
 */
sealed interface KPdfExternalOpenResult {
    data class Success(
        val location: String? = null,
    ) : KPdfExternalOpenResult

    data object Cancelled : KPdfExternalOpenResult

    data class Failure(
        val reason: KPdfError,
    ) : KPdfExternalOpenResult
}
