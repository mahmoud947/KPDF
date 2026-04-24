package com.mahmoud.kpdf_core.utils

import kotlin.io.encoding.ExperimentalEncodingApi

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

@OptIn(ExperimentalEncodingApi::class)
internal fun String.normalizedBase64(): String =
    removePrefix("data:application/pdf;base64,")
        .removePrefix("data:application/octet-stream;base64,")
        .filterNot(Char::isWhitespace)

internal fun String.fingerprint(): String {
    var hash = 1469598103934665603UL
    encodeToByteArray().forEach { byte ->
        hash = hash xor byte.toUByte().toULong()
        hash *= 1099511628211UL
    }
    return hash.toString(16)
}
