package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfPageBitmap

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal interface KPdfPageBitmapCodec {
    suspend fun encode(page: KPdfPageBitmap): ByteArray?

    suspend fun decode(
        bytes: ByteArray,
        pageIndex: Int,
    ): KPdfPageBitmap?
}
