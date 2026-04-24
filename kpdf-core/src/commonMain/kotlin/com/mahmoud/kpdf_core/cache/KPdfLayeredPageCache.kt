package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class KPdfLayeredPageCache(
    private val memoryCache: KPdfPageCache,
    private val diskCache: KPdfPageCache,
) : KPdfPageCache {
    override suspend fun get(key: KPdfPageCacheKey): KPdfPageBitmap? {
        memoryCache.get(key)?.let { return it }

        val diskPage = diskCache.get(key) ?: return null
        memoryCache.put(key, diskPage)
        return diskPage
    }

    override suspend fun put(
        key: KPdfPageCacheKey,
        page: KPdfPageBitmap,
    ) {
        memoryCache.put(key, page)
        diskCache.put(key, page)
    }

    override suspend fun removeDocument(documentKey: String) {
        memoryCache.removeDocument(documentKey)
    }

    override suspend fun clear() {
        memoryCache.clear()
        diskCache.clear()
    }
}
