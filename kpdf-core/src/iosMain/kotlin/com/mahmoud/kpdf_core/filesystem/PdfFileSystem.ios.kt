package com.mahmoud.kpdf_core.filesystem

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.posix.remove

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

actual class KPdfFileSystem {
    actual suspend fun createTempPdfFile(prefix: String): String {
        val fileName = "$prefix-${platform.Foundation.NSUUID.UUID().UUIDString}.pdf"
        return NSTemporaryDirectory().trimEnd('/') + "/" + fileName
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun createCacheDirectory(name: String): String {
        val directoryPath = NSTemporaryDirectory().trimEnd('/') + "/" + name
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directoryPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return directoryPath
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        NSFileManager.defaultManager.createFileAtPath(
            path = path,
            contents = bytes.toNSData(),
            attributes = null,
        )
    }

    actual suspend fun readBytes(path: String): ByteArray? =
        NSData.dataWithContentsOfFile(path)?.toByteArray()

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun copyFile(sourcePath: String, destinationPath: String) {
        NSFileManager.defaultManager.copyItemAtPath(sourcePath, destinationPath, error = null)
    }

    actual suspend fun exists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    actual suspend fun isReadable(path: String): Boolean =
        NSFileManager.defaultManager.isReadableFileAtPath(path)

    actual suspend fun delete(path: String): Boolean =
        remove(path) == 0
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned {
            NSData.create(
                bytes = it.addressOf(0),
                length = size.toULong(),
            )
        }
    }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isEmpty()) return bytes

    bytes.usePinned { pinned ->
        platform.posix.memcpy(
            pinned.addressOf(0),
            this.bytes,
            length,
        )
    }
    return bytes
}
