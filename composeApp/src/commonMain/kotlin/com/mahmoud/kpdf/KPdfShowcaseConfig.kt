package com.mahmoud.kpdf

import androidx.compose.runtime.Composable
import com.mahmoud.kpdf_compose.KPdfViewerToolbarConfig
import com.mahmoud.kpdf_compose.KPdfViewerToolbarIcons
import com.mahmoud.kpdf_compose.KPdfViewerToolbarStrings
import com.mahmoud.kpdf_compose.KPdfViewerToolbarVisibility
import com.mahmoud.kpdf_core.api.KPdfExternalOpenState
import com.mahmoud.kpdf_core.api.KPdfViewerConfig

/**
 * Created by Mahmoud kamal El-Din on 26/04/2026
 */
 enum class ShowcaseScreen(val label: String) {
    Remote("Remote"),
    Base64("Base64"),
    Resource("Resource"),
    Local("Local"),
    Controls("Controls"),
}

internal fun viewerConfig(): KPdfViewerConfig =
    KPdfViewerConfig.builder()
        .enableSwipe(true)
        .preloadPageCount(6)
        .zoomRange(1f, 5f)
        .doubleTapZoom(2.5f)
        .diskCacheSize(300)
        .ramCacheSize(100)
        .build()

@Composable
internal fun viewerToolbarConfig(): KPdfViewerToolbarConfig =
    KPdfViewerToolbarConfig(
        visibility = KPdfViewerToolbarVisibility(
            showPageSummary = true,
            showZoomOut = true,
            showZoomPercentage = true,
            showZoomIn = true,
            showSave = false,
            showShare = false,
            showThumbnailToggle = true,
        ),
        strings = KPdfViewerToolbarStrings.defaults(),
        icons = KPdfViewerToolbarIcons.defaults(),
    )

internal fun KPdfExternalOpenState.toStatus(): String? = when (this) {
    KPdfExternalOpenState.Idle -> null
    KPdfExternalOpenState.Exporting -> "Preparing handoff"
    is KPdfExternalOpenState.AwaitingExternalApp -> "Choose app"
    is KPdfExternalOpenState.Success -> "Opened externally"
    is KPdfExternalOpenState.Cancelled -> "Open cancelled"
    is KPdfExternalOpenState.Error -> reason.message
}
