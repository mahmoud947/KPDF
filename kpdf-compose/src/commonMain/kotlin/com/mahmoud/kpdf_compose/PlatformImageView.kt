package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@Composable
internal expect fun KPlatformImageView(
    page: KPdfPageBitmap,
    state: KPdfViewerState,
    contentDescription: String?,
    config: KPdfViewerConfig,
    modifier: Modifier = Modifier,
)
