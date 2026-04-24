package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal interface KPdfPageCache {
    suspend fun get(key: KPdfPageCacheKey): KPdfPageBitmap?

    suspend fun put(
        key: KPdfPageCacheKey,
        page: KPdfPageBitmap,
    )

    suspend fun removeDocument(documentKey: String)

    suspend fun clear()
}
