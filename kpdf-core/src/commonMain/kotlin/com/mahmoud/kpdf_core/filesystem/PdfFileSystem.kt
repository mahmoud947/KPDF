package com.mahmoud.kpdf_core.filesystem

/*
 * Platform-friendly file system boundary used by source strategies and the repository.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

expect class KPdfFileSystem {
    suspend fun createTempPdfFile(prefix: String = "kpdf-sdk"): String

    suspend fun createCacheDirectory(name: String = "kpdf-cache"): String

    suspend fun writeBytes(path: String, bytes: ByteArray)

    suspend fun readBytes(path: String): ByteArray?

    suspend fun copyFile(sourcePath: String, destinationPath: String)

    suspend fun exists(path: String): Boolean

    suspend fun isReadable(path: String): Boolean

    suspend fun delete(path: String): Boolean
}
