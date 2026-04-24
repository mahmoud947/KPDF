package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class IosKPdfPageBitmapCodec : KPdfPageBitmapCodec {
    override suspend fun encode(page: KPdfPageBitmap): ByteArray? =
        page.image.uiImage
            ?.let(::UIImagePNGRepresentation)
            ?.toByteArray()

    override suspend fun decode(
        bytes: ByteArray,
        pageIndex: Int,
    ): KPdfPageBitmap? {
        val image = runCatching { UIImage(data = bytes.toNSData()) }.getOrNull() ?: return null
        val platformImage = KPlatformImage(image)
        return KPdfPageBitmap(
            image = platformImage,
            width = platformImage.width,
            height = platformImage.height,
            pageIndex = pageIndex,
        )
    }
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
