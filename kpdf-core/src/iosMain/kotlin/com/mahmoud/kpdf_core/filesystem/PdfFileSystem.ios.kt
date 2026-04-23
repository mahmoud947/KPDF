package com.mahmoud.kpdf_core.filesystem

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
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

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        NSFileManager.defaultManager.createFileAtPath(
            path = path,
            contents = bytes.toNSData(),
            attributes = null,
        )
    }

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

@OptIn(ExperimentalForeignApi::class)
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
