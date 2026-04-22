package com.mahmoud.kpdf_core.api

public sealed interface KPdfSource {
    /**
     * Remote PDF loaded through the SDK source pipeline.
     *
     * Headers are part of the source identity because they can affect the
     * returned document for authenticated or negotiated responses.
     */
    public data class Url(
        public val url: String,
        public val headers: Map<String, String> = emptyMap(),
    ) : KPdfSource

}
