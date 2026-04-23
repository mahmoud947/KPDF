package com.mahmoud.kpdf_core.network

import com.mahmoud.kpdf_core.error.KPdfHttpException
import com.mahmoud.kpdf_core.error.KPdfIoException
import kotlinx.coroutines.ensureActive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

actual class KPdfRemoteDataSource {
    actual suspend fun downloadToFile(
        url: String,
        headers: Map<String, String>,
        destinationPath: String
    ): Result<KPdfRemoteDownloadResult> =
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
            }

            try {
                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    throw KPdfHttpException(statusCode)
                }

                val target = File(destinationPath)
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                        }
                    }
                }

                KPdfRemoteDownloadResult(
                    filePath = destinationPath,
                    contentLength = connection.contentLengthLong.takeIf { it >= 0 },
                )
            } finally {
                connection.disconnect()
            }
        }.recoverCatching {
            if (it is KPdfHttpException) throw it
            throw KPdfIoException("Unable to download PDF.", it)
        }
}

