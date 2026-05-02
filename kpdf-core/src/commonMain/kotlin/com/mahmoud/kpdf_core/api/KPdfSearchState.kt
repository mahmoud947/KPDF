package com.mahmoud.kpdf_core.api

/**
 * Observable state for text search across the currently opened PDF.
 */
sealed interface KPdfSearchState {
    data object Idle : KPdfSearchState

    data class Searching(
        val query: String,
    ) : KPdfSearchState

    data class Success(
        val query: String,
        val results: List<KPdfSearchResult>,
        val activeResultIndex: Int = results.firstIndexOrZero(),
    ) : KPdfSearchState {
        val activeResult: KPdfSearchResult? = results.getOrNull(activeResultIndex)
    }

    data class Error(
        val query: String,
        val reason: KPdfError,
    ) : KPdfSearchState
}

data class KPdfSearchResult(
    val pageIndex: Int,
    val matchIndexOnPage: Int,
    val textStartIndex: Int,
    val bounds: List<KPdfSearchRect>,
)

data class KPdfSearchRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun List<KPdfSearchResult>.firstIndexOrZero(): Int =
    if (isEmpty()) -1 else 0
