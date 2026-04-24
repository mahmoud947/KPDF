package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class KPdfDiskPageCache(
    private val storage: KPdfDiskCacheStorage,
    private val bitmapCodec: KPdfPageBitmapCodec,
    private val maxEntries: Int,
    private val cacheDirectoryName: String = DefaultCacheDirectoryName,
) : KPdfPageCache {
    private val mutex = Mutex()
    private var cacheDirectoryPath: String? = null

    override suspend fun get(key: KPdfPageCacheKey): KPdfPageBitmap? {
        if (maxEntries <= 0) return null

        return mutex.withLock {
            runCatching {
                val cacheDirectory = resolveCacheDirectoryLocked()
                val fileName = fileNameFor(key)
                val filePath = buildPath(cacheDirectory, fileName)

                if (!storage.exists(filePath)) {
                    removeFromIndexLocked(cacheDirectory, fileName)
                    return@withLock null
                }

                val bytes = storage.readBytes(filePath)
                    ?: run {
                        removeEntryLocked(cacheDirectory, fileName)
                        return@withLock null
                    }

                val decoded = bitmapCodec.decode(bytes, pageIndex = key.pageIndex)
                    ?: run {
                        removeEntryLocked(cacheDirectory, fileName)
                        return@withLock null
                    }

                updateAccessOrderLocked(cacheDirectory, fileName)
                decoded
            }.getOrNull()
        }
    }

    override suspend fun put(
        key: KPdfPageCacheKey,
        page: KPdfPageBitmap,
    ) {
        if (maxEntries <= 0) return

        val encodedPage = runCatching { bitmapCodec.encode(page) }.getOrNull() ?: return

        mutex.withLock {
            runCatching {
                val cacheDirectory = resolveCacheDirectoryLocked()
                val fileName = fileNameFor(key)
                storage.writeBytes(buildPath(cacheDirectory, fileName), encodedPage)
                updateAccessOrderLocked(cacheDirectory, fileName)
                trimToSizeLocked(cacheDirectory)
            }
        }
    }

    override suspend fun removeDocument(documentKey: String) {
        mutex.withLock {
            runCatching {
                val cacheDirectory = resolveCacheDirectoryLocked()
                val documentPrefix = filePrefixFor(documentKey)
                val index = readIndexLocked(cacheDirectory)
                val updatedIndex = index.filterNot { fileName ->
                    if (!fileName.startsWith(documentPrefix)) {
                        false
                    } else {
                        storage.delete(buildPath(cacheDirectory, fileName))
                        true
                    }
                }
                writeIndexLocked(cacheDirectory, updatedIndex)
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            runCatching {
                val cacheDirectory = resolveCacheDirectoryLocked()
                readIndexLocked(cacheDirectory).forEach { fileName ->
                    storage.delete(buildPath(cacheDirectory, fileName))
                }
                writeIndexLocked(cacheDirectory, emptyList())
            }
        }
    }

    private suspend fun resolveCacheDirectoryLocked(): String {
        return cacheDirectoryPath ?: storage.createCacheDirectory(cacheDirectoryName).also {
            cacheDirectoryPath = it
        }
    }

    private suspend fun trimToSizeLocked(cacheDirectory: String) {
        val index = readIndexLocked(cacheDirectory).toMutableList()
        while (index.size > maxEntries) {
            val oldest = index.removeFirstOrNull() ?: break
            storage.delete(buildPath(cacheDirectory, oldest))
        }
        writeIndexLocked(cacheDirectory, index)
    }

    private suspend fun updateAccessOrderLocked(
        cacheDirectory: String,
        fileName: String,
    ) {
        val index = readIndexLocked(cacheDirectory)
            .filterNot { it == fileName } + fileName
        writeIndexLocked(cacheDirectory, index)
    }

    private suspend fun removeFromIndexLocked(
        cacheDirectory: String,
        fileName: String,
    ) {
        val updatedIndex = readIndexLocked(cacheDirectory)
            .filterNot { it == fileName }
        writeIndexLocked(cacheDirectory, updatedIndex)
    }

    private suspend fun removeEntryLocked(
        cacheDirectory: String,
        fileName: String,
    ) {
        storage.delete(buildPath(cacheDirectory, fileName))
        removeFromIndexLocked(cacheDirectory, fileName)
    }

    private suspend fun readIndexLocked(cacheDirectory: String): List<String> {
        val rawIndex = storage.readBytes(buildPath(cacheDirectory, IndexFileName))
            ?.decodeToString()
            ?: return emptyList()

        return rawIndex.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private suspend fun writeIndexLocked(
        cacheDirectory: String,
        index: List<String>,
    ) {
        storage.writeBytes(
            path = buildPath(cacheDirectory, IndexFileName),
            bytes = index.joinToString(separator = "\n").encodeToByteArray(),
        )
    }

    private fun fileNameFor(key: KPdfPageCacheKey): String {
        return buildString {
            append(filePrefixFor(key.documentKey))
            append(key.pageIndex)
            append('-')
            append(key.targetWidth)
            append('x')
            append(key.targetHeight)
            append('-')
            append(key.zoom.toBits().toString(16))
            append(CacheFileExtension)
        }
    }

    private fun filePrefixFor(documentKey: String): String =
        "${fingerprint(documentKey)}-"

    private fun buildPath(
        directoryPath: String,
        fileName: String,
    ): String = directoryPath.trimEnd('/') + "/" + fileName

    private fun fingerprint(value: String): String {
        var hash = 1469598103934665603UL
        value.encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toULong()
            hash *= 1099511628211UL
        }
        return hash.toString(16)
    }

    private companion object {
        const val DefaultCacheDirectoryName = "kpdf-page-cache"
        const val IndexFileName = "index.txt"
        const val CacheFileExtension = ".page"
    }
}
