package com.mahmoud.kpdf_core.cache

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal data class KPdfPageCacheKey(
    val documentId: String,
    val pageIndex: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val zoom: Float,
)