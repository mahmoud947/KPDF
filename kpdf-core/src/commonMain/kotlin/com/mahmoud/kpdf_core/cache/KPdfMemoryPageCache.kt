package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */


internal class KPdfMemoryPageCache(
    private val maxEntries: Int,
) : KPdfPageCache {
    private val mutex = Mutex()
    private val entries = LinkedHashMap<KPdfPageCacheKey, KPdfPageBitmap>()

    override suspend fun get(key: KPdfPageCacheKey): KPdfPageBitmap? = mutex.withLock {
        if (maxEntries <= 0) return@withLock null

        val cachedPage = entries.remove(key) ?: return@withLock null
        entries[key] = cachedPage
        cachedPage
    }

    override suspend fun put(
        key: KPdfPageCacheKey,
        page: KPdfPageBitmap,
    ) {
        if (maxEntries <= 0) return

        mutex.withLock {
            entries.remove(key)
            entries[key] = page
            trimToSizeLocked()
        }
    }

    override suspend fun removeDocument(documentKey: String) {
        mutex.withLock {
            val keysToRemove = entries.keys
                .filter { it.documentKey == documentKey }

            keysToRemove.forEach(entries::remove)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            entries.clear()
        }
    }

    private fun trimToSizeLocked() {
        while (entries.size > maxEntries) {
            val oldestKey = entries.entries.iterator().next().key
            entries.remove(oldestKey)
        }
    }
}
