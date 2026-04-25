package com.mahmoud.kpdf_compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mahmoud.kpdf_core.api.KPdfPageBitmap

@Composable
internal actual fun KThumbnailImageView(
    page: KPdfPageBitmap,
    contentDescription: String?,
    modifier: Modifier,
) {
    val bitmap = page.image.bitmap ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.Low,
    )
}
