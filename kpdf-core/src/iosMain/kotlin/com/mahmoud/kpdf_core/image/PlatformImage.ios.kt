package com.mahmoud.kpdf_core.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIImage

 actual class PlatformImage actual constructor(
     actual val width: Int,
     actual val height: Int,
) {
     var uiImage: UIImage? = null
        private set

    @OptIn(ExperimentalForeignApi::class)
    constructor(uiImage: UIImage) : this(
        width = uiImage.size.useContents { width.toInt().coerceAtLeast(1) },
        height = uiImage.size.useContents { height.toInt().coerceAtLeast(1) },
    ) {
        this.uiImage = uiImage
    }
}
