package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 22/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
sealed interface KPdfError {
    val message: String


    data class FileNotFound(
        val path: String? = null,
    ) : KPdfError {
        override val message: String = path?.let { "PDF file was not found: $it" }
            ?: "PDF file was not found."
    }

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

    data object Unauthorized : KPdfError {
        override val message: String = "The PDF request is unauthorized."
    }

    data object NotFound : KPdfError {
        override val message: String = "The requested PDF was not found."
    }

    data object CorruptedDocument : KPdfError {
        override val message: String = "The PDF document appears to be corrupted."
    }

    data class Unknown(
        override val message: String,
    ) : KPdfError

}

class KPdfException(
    val error: KPdfError,
    cause: Throwable? = null,
) : Exception(error.message, cause)