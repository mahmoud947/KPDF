package com.mahmoud.kpdf_core.network

import com.mahmoud.kpdf_core.error.KPdfIoException
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

actual class KPdfRemoteDataSource {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun downloadToFile(
        url: String,
        headers: Map<String, String>,
        destinationPath: String
    ): Result<KPdfRemoteDownloadResult> = runCatching {
        val nsUrl = NSURL.URLWithString(url)
            ?: throw KPdfIoException("Invalid PDF URL: $url")

        val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
            HTTPMethod = "GET"
            headers.forEach { (key, value) ->
                setValue(value, forHTTPHeaderField = key)
            }
        }

        val data = suspendCoroutine<NSData> { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
                when {
                    error != null -> {
                        continuation.resumeWith(
                            Result.failure(
                                KPdfIoException(error.localizedDescription ?: "Unable to download PDF.")
                            )
                        )
                    }

                    response is NSHTTPURLResponse && response.statusCode.toInt() !in 200..299 -> {
                        continuation.resumeWith(
                            Result.failure(
                                KPdfIoException("HTTP ${response.statusCode}: Failed to download PDF.")
                            )
                        )
                    }

                    data != null -> {
                        continuation.resume(data)
                    }

                    else -> {
                        continuation.resumeWith(
                            Result.failure(
                                KPdfIoException("Empty response while downloading PDF.")
                            )
                        )
                    }
                }
            }
            task.resume()
        }

        val wrote = NSFileManager.defaultManager.createFileAtPath(
            path = destinationPath,
            contents = data,
            attributes = null,
        )

        if (!wrote) {
            throw KPdfIoException("Unable to write downloaded PDF.")
        }

        KPdfRemoteDownloadResult(
            filePath = destinationPath,
            contentLength = data.length.toLong(),
        )
    }
}