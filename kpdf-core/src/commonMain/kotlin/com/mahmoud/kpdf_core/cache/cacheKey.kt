package com.mahmoud.kpdf_core.cache

import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.utils.fingerprint
import com.mahmoud.kpdf_core.utils.normalizedBase64

internal fun KPdfSource.cacheKey(): String = when (this) {
    is KPdfSource.Url -> {
        buildString {
            append("url:")
            append(url)

            if (headers.isNotEmpty()) {
                append("|headers:")
                headers.entries
                    .sortedBy { it.key }
                    .forEach { entry ->
                        append(entry.key)
                        append('=')
                        append(entry.value)
                        append(';')
                    }
            }
        }
    }

    is KPdfSource.Bytes -> "bytes:${data.fingerprint()}"
    is KPdfSource.Base64 -> "base64:${value.normalizedBase64().fingerprint()}"
}
