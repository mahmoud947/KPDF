package com.mahmoud.kpdf.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mahmoud.kpdf.common.ScreenTitle
import com.mahmoud.kpdf.common.SimpleCard
import com.mahmoud.kpdf.common.StatusText
import com.mahmoud.kpdf.common.ViewerContent
import com.mahmoud.kpdf_core.api.KPdfViewerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text

/**
 * Created by Mahmoud kamal El-Din on 26/04/2026
 */
@Composable
fun LocalScreen(
    state: KPdfViewerState,
    hasImportedSource: Boolean,
    thumbnailsVisible: Boolean,
    onToggleThumbnails: (Boolean) -> Unit,
    onImportClick: () -> Unit,
) {
    if (!hasImportedSource) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            SimpleCard {
                ScreenTitle("Local", "Import from device")
                StatusText("No local PDF selected")
                Button(onClick = onImportClick) {
                    Text("Import")
                }
            }
        }
        return
    }

    ViewerContent(
        state = state,
        thumbnailsVisible = thumbnailsVisible,
        onToggleThumbnails = onToggleThumbnails,
        onImportClick = onImportClick,
    )
}
