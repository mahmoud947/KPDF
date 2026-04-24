package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.filesystem.KPdfFileSystem

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal interface KPdfDiskCacheStorage {
    suspend fun createCacheDirectory(name: String): String

    suspend fun writeBytes(path: String, bytes: ByteArray)

    suspend fun readBytes(path: String): ByteArray?

    suspend fun exists(path: String): Boolean

    suspend fun delete(path: String): Boolean
}

internal class KPdfFileSystemDiskCacheStorage(
    private val fileSystem: KPdfFileSystem,
) : KPdfDiskCacheStorage {
    override suspend fun createCacheDirectory(name: String): String =
        fileSystem.createCacheDirectory(name)

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        fileSystem.writeBytes(path, bytes)
    }

    override suspend fun readBytes(path: String): ByteArray? =
        fileSystem.readBytes(path)

    override suspend fun exists(path: String): Boolean =
        fileSystem.exists(path)

    override suspend fun delete(path: String): Boolean =
        fileSystem.delete(path)
}
