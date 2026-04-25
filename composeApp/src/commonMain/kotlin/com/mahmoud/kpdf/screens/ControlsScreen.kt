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
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_core.api.KPdfViewerState

/**
 * Created by Mahmoud kamal El-Din on 26/04/2026
 */
@Composable
fun ControlsScreen(
    state: KPdfViewerState,
    thumbnailsVisible: Boolean,
    onToggleThumbnails: () -> Unit,
    onShare: () -> Unit,
    onImportClick: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KPdfViewer(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
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
            secondLabel = "Retry",
            secondAction = { state.retry() },
            thirdLabel = "Reset",
            thirdAction = { state.resetZoom() },
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
