package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 22/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
sealed interface KPdfError {
    val message: String

}

class PdfException(
    val error: KPdfError,
    cause: Throwable? = null,
) : Exception(error.message, cause)