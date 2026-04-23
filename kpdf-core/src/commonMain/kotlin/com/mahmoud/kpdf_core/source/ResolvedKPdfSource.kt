package com.mahmoud.kpdf_core.source


/*
 * Normalized source output consumed by the repository.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
sealed interface ResolvedKPdfSource {

    data class Bytes(
        val data: ByteArray,
    ) : ResolvedKPdfSource {
        override fun equals(other: Any?): Boolean =
            this === other || other is Bytes && data.contentEquals(other.data)
        override fun hashCode(): Int = data.contentHashCode()
    }


     data class File(
         val path: String,
         val temporary: Boolean = false,
    ) : ResolvedKPdfSource
}
