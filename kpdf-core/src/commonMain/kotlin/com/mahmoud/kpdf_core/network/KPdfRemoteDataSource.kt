package com.mahmoud.kpdf_core.network

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 *
 * Network abstraction for downloading remote PDF files.
 * Implementations should stream bytes into [destinationPath] and cooperate with
 * coroutine cancellation. They should avoid loading large PDFs into strings.
 */
expect class KPdfRemoteDataSource {
    suspend fun downloadToFile(
        url: String,
        headers: Map<String, String>,
        destinationPath: String,
    ): Result<KPdfRemoteDownloadResult>
}

data class KPdfRemoteDownloadResult(
    val filePath: String,
    val contentLength: Long? = null,
)
