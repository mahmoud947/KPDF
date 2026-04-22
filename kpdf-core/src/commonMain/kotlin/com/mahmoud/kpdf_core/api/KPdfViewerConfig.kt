package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
class KPdfViewerConfig internal constructor(
    val enableZoom: Boolean,
) {
    /**
     * Mutable builder for [KPdfViewerConfig].
     */
    class Builder {
        private var enableZoom: Boolean = true


        fun enableZoom(value: Boolean): Builder = apply {
            enableZoom = value
        }

        fun build(): KPdfViewerConfig {


            return KPdfViewerConfig(
                enableZoom = enableZoom,
            )
        }
    }

    companion object {
        /**
         * Starts a new [KPdfViewerConfig] builder.
         */
        fun builder(): Builder = Builder()
    }
}

/**
 * Kotlin DSL for creating [KPdfViewerConfig].
 */
fun pdfViewerConfig(block: KPdfViewerConfig.Builder.() -> Unit): KPdfViewerConfig =
    KPdfViewerConfig.builder()
        .apply(block)
        .build()
