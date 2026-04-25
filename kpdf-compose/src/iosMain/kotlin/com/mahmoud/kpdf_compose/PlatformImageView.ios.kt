package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit

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
    val uiImage = page.image.uiImage ?: return

    if (!config.enableZoom) {
        UIKitView(
            factory = {
                UIImageView().apply {
                    contentMode = UIViewContentModeScaleAspectFit
                    clipsToBounds = true
                    userInteractionEnabled = false
                }
            },
            modifier = modifier,
            update = { imageView ->
                imageView.image = uiImage
            },
            onRelease = {},
            properties = UIKitInteropProperties(
                isInteractive = false,
                isNativeAccessibilityEnabled = true
            )
        )
        return
    }

    UIKitView(
        factory = {
            ZoomableImageContainer(
                frame = CGRectZero.readValue(),
                minZoom = config.minZoom.toDouble(),
                maxZoom = config.maxZoom.toDouble(),
                doubleTapZoom = config.doubleTapZoom.toDouble()
            ).apply {
                onZoomChanged = { zoom ->
                    state.setZoom(zoom.toFloat())
                }
            }
        },
        modifier = modifier,
        update = { container ->
            container.setImage(uiImage)
            container.setExternalZoom(sharedZoom.toDouble(), animated = false)
        },
        onRelease = {},
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}
