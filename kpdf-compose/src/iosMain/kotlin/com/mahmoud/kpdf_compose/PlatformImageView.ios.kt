package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfSearchState
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import androidx.compose.ui.viewinterop.UIKitView

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun KPlatformImageView(
    page: KPdfPageBitmap,
    state: KPdfViewerState,
    contentDescription: String?,
    config: KPdfViewerConfig,
    modifier: Modifier
) {
    val sharedZoom by state.currentZoom.collectAsState()
    val searchState by state.searchState.collectAsState()
    val uiImage = page.image.uiImage ?: return
    val minZoom = config.minZoom.toDouble()
    val maxZoom = if (config.enableZoom) config.maxZoom.toDouble() else minZoom

    UIKitView(
        factory = {
            ZoomableImageContainer(
                frame = CGRectZero.readValue(),
                minZoom = minZoom,
                maxZoom = maxZoom,
                doubleTapZoom = if (config.enableZoom) config.doubleTapZoom.toDouble() else minZoom,
                swipeEnabled = config.enableZoom && config.enableSwipe,
            ).apply {
                onZoomChanged = { zoom ->
                    if (config.enableZoom) {
                        state.setZoom(zoom.toFloat())
                    }
                }
                onSwipeNext = {
                    state.nextPage()
                }
                onSwipePrevious = {
                    state.previousPage()
                }
            }
        },
        modifier = modifier,
        update = { container ->
            container.setImage(uiImage)
            container.setSearchHighlights(page.searchHighlights(searchState))
            container.setExternalZoom(
                scale = if (config.enableZoom) sharedZoom.toDouble() else minZoom,
                animated = false,
            )
        },
        onRelease = {},
        properties = UIKitInteropProperties(
            isInteractive = config.enableZoom,
            isNativeAccessibilityEnabled = true
        )
    )
}

private fun KPdfPageBitmap.searchHighlights(
    searchState: KPdfSearchState,
): List<KPdfSearchHighlight> {
    val success = searchState as? KPdfSearchState.Success ?: return emptyList()
    return success.results
        .filter { it.pageIndex == pageIndex }
        .flatMap { result ->
            result.bounds.map { rect ->
                KPdfSearchHighlight(
                    rect = rect,
                    active = success.activeResult == result,
                )
            }
        }
}
