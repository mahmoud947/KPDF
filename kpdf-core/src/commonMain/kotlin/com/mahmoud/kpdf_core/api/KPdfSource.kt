package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

 sealed interface KPdfSource {
    /**
     * Remote PDF loaded through the SDK source pipeline.
     *
     * Headers are part of the source identity because they can affect the
     * returned document for authenticated or negotiated responses.
     */
     data class Url(
         val url: String,
         val headers: Map<String, String> = emptyMap(),
    ) : KPdfSource

    /**
     * Raw PDF bytes already loaded by the caller.
     */
    data class Bytes(
        val data: ByteArray,
    ) : KPdfSource {
        override fun equals(other: Any?): Boolean =
            this === other || other is Bytes && data.contentEquals(other.data)

        override fun hashCode(): Int = data.contentHashCode()
    }

    /**
     * Inline Base64-encoded PDF content.
     *
     * Supports both raw Base64 strings and data URLs such as
     * `data:application/pdf;base64,...`.
     */
    data class Base64(
        val value: String,
    ) : KPdfSource
}
