package com.mahmoud.kpdf_compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSData
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@Composable
internal actual fun KPlatformImageView(
    image: KPlatformImage,
    contentDescription: String?,
    modifier: Modifier
) {
    val uiImage = image.uiImage ?: return
    val bitmap = remember(image) {
        UIImagePNGRepresentation(uiImage)?.toByteArray()?.let { bytes ->
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isEmpty()) return bytes
    bytes.usePinned {
        memcpy(it.addressOf(0), this.bytes, length.convert())
    }
    return bytes
}
