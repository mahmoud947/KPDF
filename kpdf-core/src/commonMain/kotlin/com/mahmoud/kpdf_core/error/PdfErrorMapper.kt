package com.mahmoud.kpdf_core.error

import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
 interface PdfErrorMapper {
     fun map(throwable: Throwable): KPdfError
}

 object DefaultPdfErrorMapper : PdfErrorMapper {
    override fun map(throwable: Throwable): KPdfError =
        when (throwable) {
            is KPdfException -> throwable.error
            is KPdfHttpException -> throwable.toPdfError()
            is KPdfIoException -> KPdfError.NetworkError(reason = throwable.message)
            else -> KPdfError.Unknown(throwable.message ?: "Unknown PDF error.")
        }
}

internal class KPdfHttpException(
    val statusCode: Int,
    message: String? = null,
) : Exception(message ?: "HTTP $statusCode")

internal class KPdfIoException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

private fun KPdfHttpException.toPdfError(): KPdfError =
    when (statusCode) {
        401, 403 -> KPdfError.Unauthorized
        404 -> KPdfError.NotFound
        else -> KPdfError.NetworkError(statusCode = statusCode, reason = message)
    }

internal fun kpdfFailure(error: KPdfError): Result<Nothing> =
    Result.failure(KPdfException(error))

internal fun kpdfFailure(throwable: Throwable, mapper: PdfErrorMapper = DefaultPdfErrorMapper): Result<Nothing> =
    kpdfFailure(mapper.map(throwable))
