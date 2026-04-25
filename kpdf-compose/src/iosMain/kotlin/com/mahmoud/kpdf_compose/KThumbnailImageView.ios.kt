package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun KThumbnailImageView(
    page: KPdfPageBitmap,
    contentDescription: String?,
    modifier: Modifier,
) {
    val uiImage = page.image.uiImage ?: return

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
            isNativeAccessibilityEnabled = true,
        ),
    )
}
