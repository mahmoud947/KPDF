package com.mahmoud.kpdf_core.api

import com.mahmoud.kpdf_core.image.KPlatformImage


/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

data class KPdfPageBitmap(
    val image: KPlatformImage,
    override val width: Int,
    override val height: Int,
    val pageIndex: Int,
) : KPdfPageImage
