package com.mahmoud.kpdf.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf.viewerToolbarConfig
import com.mahmoud.kpdf_compose.KPdfThumbnailStrip
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_compose.KPdfViewerToolbar
import com.mahmoud.kpdf_core.api.KPdfViewerState

@Composable
fun ViewerContent(
    state: KPdfViewerState,
    thumbnailsVisible: Boolean,
    onToggleThumbnails: (Boolean) -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KPdfViewerToolbar(
            state = state,
            isThumbnailStripVisible = thumbnailsVisible,
            onThumbnailToggle = onToggleThumbnails,
            modifier = Modifier.fillMaxWidth(),
            config = viewerToolbarConfig(),
        )
        KPdfViewer(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        if (thumbnailsVisible) {
            KPdfThumbnailStrip(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
        QuickActionRow(
            firstLabel = "Prev",
            firstAction = { state.previousPage() },
            secondLabel = "Next",
            secondAction = { state.nextPage() },
            thirdLabel = "Import",
            thirdAction = onImportClick,
        )
    }
}
