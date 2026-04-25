package com.mahmoud.kpdf.screens

import androidx.compose.runtime.Composable
import com.mahmoud.kpdf.common.ViewerContent
import com.mahmoud.kpdf_core.api.KPdfViewerState

/**
 * Created by Mahmoud kamal El-Din on 26/04/2026
 */
@Composable
fun Base64Screen(
    state: KPdfViewerState,
    thumbnailsVisible: Boolean,
    onToggleThumbnails: (Boolean) -> Unit,
    onImportClick: () -> Unit,
) {
    ViewerContent(
        state = state,
        thumbnailsVisible = thumbnailsVisible,
        onToggleThumbnails = onToggleThumbnails,
        onImportClick = onImportClick,
    )
}
