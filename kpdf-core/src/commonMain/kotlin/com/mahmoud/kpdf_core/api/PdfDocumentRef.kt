package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 22/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */


interface PdfDocumentRef {
    val id: String
    val pageCount: Int
    val title: String?


    /**
     * Renders a page into an opaque platform bitmap.
     */
    suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float = 1f,
    ): Result<PdfPageBitmap>


}
