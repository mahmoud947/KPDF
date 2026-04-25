package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mahmoud.kpdf_core.api.KPdfPageBitmap

@Composable
internal expect fun KThumbnailImageView(
    page: KPdfPageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
)
