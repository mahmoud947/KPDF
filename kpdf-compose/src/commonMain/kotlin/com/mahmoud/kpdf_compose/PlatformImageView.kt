package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mahmoud.kpdf_core.image.KPlatformImage

@Composable
internal expect fun KPlatformImageView(
    image: KPlatformImage,
    contentDescription: String?,
    modifier: Modifier = Modifier,
)
