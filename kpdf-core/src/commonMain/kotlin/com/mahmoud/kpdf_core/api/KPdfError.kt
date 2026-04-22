package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 22/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
sealed interface KPdfError {
    val message: String


    data class NetworkError(
        val statusCode: Int? = null,
        val reason: String? = null,
    ) : KPdfError {
        override val message: String = when {
            statusCode != null && reason != null -> "Network error ($statusCode): $reason"
            statusCode != null -> "Network error ($statusCode)."
            reason != null -> "Network error: $reason"
            else -> "Network error while loading the PDF."
        }
    }

    data class Unknown(
        override val message: String,
    ) : KPdfError

}

class KPdfException(
    val error: KPdfError,
    cause: Throwable? = null,
) : Exception(error.message, cause)