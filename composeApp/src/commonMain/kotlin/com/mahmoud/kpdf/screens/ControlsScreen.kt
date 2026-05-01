package com.mahmoud.kpdf.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf.common.QuickActionRow
import com.mahmoud.kpdf_compose.KPdfThumbnailStrip
import com.mahmoud.kpdf_compose.KPdfVerticalViewer
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_core.api.KPdfViewerState

/**
 * Created by Mahmoud kamal El-Din on 26/04/2026
 */
@Composable
fun ControlsScreen(
    state: KPdfViewerState,
    thumbnailsVisible: Boolean,
    verticalView: Boolean,
    onToggleThumbnails: () -> Unit,
    onToggleVerticalView: () -> Unit,
    onShare: () -> Unit,
    onImportClick: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (verticalView) {
            KPdfVerticalViewer(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            KPdfViewer(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
        QuickActionRow(
            firstLabel = "Share",
            firstAction = onShare,
            secondLabel = "Open App",
            secondAction = onOpenExternal,
            thirdLabel = "Import",
            thirdAction = onImportClick,
        )
        QuickActionRow(
            firstLabel = if (thumbnailsVisible) "Hide" else "Thumbs",
            firstAction = onToggleThumbnails,
            secondLabel = if (verticalView) "Paged" else "Vertical",
            secondAction = onToggleVerticalView,
            thirdLabel = "Reset",
            thirdAction = { state.resetZoom() },
        )
        QuickActionRow(
            firstLabel = "Prev",
            firstAction = { state.previousPage() },
            secondLabel = "Next",
            secondAction = { state.nextPage() },
            thirdLabel = "Retry",
            thirdAction = { state.retry() },
        )
        if (thumbnailsVisible) {
            KPdfThumbnailStrip(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}
