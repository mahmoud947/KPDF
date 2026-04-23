package com.mahmoud.kpdf_compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mahmoud.kpdf_core.image.KPlatformImage

@Composable
internal actual fun KPlatformImageView(
    image: KPlatformImage,
    contentDescription: String?,
    modifier: Modifier
) {
    val bitmap = image.bitmap ?: return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}