package com.mahmoud.kpdf_core.api

/**
 * Save result reported back to [KPdfViewerState] by platform compose integrations.
 */
sealed interface KPdfSaveResult {
    data class Success(
        val location: String? = null,
    ) : KPdfSaveResult

    data class Failure(
        val reason: KPdfError,
    ) : KPdfSaveResult

    data object Cancelled : KPdfSaveResult
}
