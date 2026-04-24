package com.mahmoud.kpdf_core.api

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

}

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
}
