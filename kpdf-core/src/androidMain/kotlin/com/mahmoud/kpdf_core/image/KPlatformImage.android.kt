package com.mahmoud.kpdf_core.image

import android.graphics.Bitmap

actual class KPlatformImage actual constructor(
    actual val width: Int,
    actual val height: Int,
) {
    var bitmap: Bitmap? = null
        private set

    constructor(bitmap: Bitmap) : this(bitmap.width, bitmap.height) {
        this.bitmap = bitmap
    }
}
