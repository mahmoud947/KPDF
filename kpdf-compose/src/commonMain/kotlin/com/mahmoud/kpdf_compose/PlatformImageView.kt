package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mahmoud.kpdf_core.image.KPlatformImage

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@Composable
internal expect fun KPlatformImageView(
    image: KPlatformImage,
    contentDescription: String?,
    modifier: Modifier = Modifier,
)
