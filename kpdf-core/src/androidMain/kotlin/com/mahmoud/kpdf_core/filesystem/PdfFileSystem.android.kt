package com.mahmoud.kpdf_core.filesystem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

actual class KPdfFileSystem(
    private val tempDirectory: File = File(System.getProperty("java.io.tmpdir") ?: "."),
) {
    actual suspend fun createTempPdfFile(prefix: String): String = withContext(Dispatchers.IO) {
        tempDirectory.mkdirs()
        File.createTempFile(prefix, ".pdf", tempDirectory).absolutePath
    }

    actual suspend fun createCacheDirectory(name: String): String = withContext(Dispatchers.IO) {
        File(tempDirectory, name).apply { mkdirs() }.absolutePath
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            File(path).writeBytes(bytes)
        }
    }

    actual suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        file.readBytes()
    }

    actual suspend fun copyFile(sourcePath: String, destinationPath: String) {
        withContext(Dispatchers.IO) {
            File(sourcePath).copyTo(File(destinationPath), overwrite = true)
        }
    }

    actual suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    actual suspend fun isReadable(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).canRead()
    }

    actual suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).delete()
    }
}
