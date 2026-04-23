package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun KPlatformImageView(
    image: KPlatformImage,
    contentDescription: String?,
    modifier: Modifier
) {
    val uiImage = image.uiImage ?: return

    UIKitView(
        factory = {
            ZoomableImageContainer(
                frame = CGRectZero.readValue(),
                minZoom = 1.0,
                maxZoom = 5.0,
                scaleStep = 2.0
            )
        },
        modifier = modifier,
        update = { container ->
            container.setImage(uiImage)
        },
        onRelease = {},
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}
