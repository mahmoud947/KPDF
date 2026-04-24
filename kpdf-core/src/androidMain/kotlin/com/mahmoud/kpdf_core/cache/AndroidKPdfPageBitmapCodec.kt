package com.mahmoud.kpdf_core.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class AndroidKPdfPageBitmapCodec : KPdfPageBitmapCodec {
    override suspend fun encode(page: KPdfPageBitmap): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = page.image.bitmap ?: return@withContext null
        ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                return@withContext null
            }
            output.toByteArray()
        }
    }

    override suspend fun decode(
        bytes: ByteArray,
        pageIndex: Int,
    ): KPdfPageBitmap? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        KPdfPageBitmap(
            image = KPlatformImage(bitmap),
            width = bitmap.width,
            height = bitmap.height,
            pageIndex = pageIndex,
        )
    }
}
