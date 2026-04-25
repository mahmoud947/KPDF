package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
class KPdfViewerConfig internal constructor(
    val enableZoom: Boolean,
    val enableSwipe: Boolean,
    val minZoom: Float,
    val maxZoom: Float,
    val doubleTapZoom: Float,
    val ramCacheSize: Int,
    val diskCacheSize: Int,
    val preloadPageCount: Int,
) {
    /**
     * Mutable builder for [KPdfViewerConfig].
     */
    class Builder {
        private var enableZoom: Boolean = true
        private var enableSwipe: Boolean = true
        private var minZoom: Float = 1f
        private var maxZoom: Float = 4f
        private var doubleTapZoom: Float = 2f
        private var ramCacheSize: Int = DefaultRamCacheSize
        private var diskCacheSize: Int = DefaultDiskCacheSize
        private var preloadPageCount: Int = 0

        fun enableZoom(value: Boolean): Builder = apply {
            enableZoom = value
        }

        /**
         * Enables page navigation by horizontal swipe when the viewer is at its base zoom.
         */
        fun enableSwipe(value: Boolean): Builder = apply {
            enableSwipe = value
        }

        /**
         * Sets the pinch zoom range used by Compose PDF content.
         */
        fun zoomRange(minZoom: Float, maxZoom: Float): Builder = apply {
            require(minZoom > 0f) { "minZoom must be greater than 0." }
            require(maxZoom >= minZoom) { "maxZoom must be greater than or equal to minZoom." }

            this.minZoom = minZoom
            this.maxZoom = maxZoom
        }

        /**
         * Sets the zoom level used when the page is double-tapped.
         */
        fun doubleTapZoom(value: Float): Builder = apply {
            require(value > 0f) { "doubleTapZoom must be greater than 0." }

            doubleTapZoom = value
        }

        /**
         * Sets the max number of rendered pages held in RAM for this viewer.
         *
         * Use `0` to disable the memory cache.
         */
        fun ramCacheSize(value: Int): Builder = apply {
            require(value >= 0) { "ramCacheSize must be greater than or equal to 0." }

            ramCacheSize = value
        }

        /**
         * Sets the max number of rendered pages held on disk for this viewer.
         *
         * Use `0` to disable the disk cache.
         */
        fun diskCacheSize(value: Int): Builder = apply {
            require(value >= 0) { "diskCacheSize must be greater than or equal to 0." }

            diskCacheSize = value
        }

        /**
         * Sets how many nearby pages should be preloaded around the current page.
         *
         * Use `0` to disable background preloading.
         */
        fun preloadPageCount(value: Int): Builder = apply {
            require(value >= 0) { "preloadPageCount must be greater than or equal to 0." }

            preloadPageCount = value
        }

        fun build(): KPdfViewerConfig {
            return KPdfViewerConfig(
                enableZoom = enableZoom,
                enableSwipe = enableSwipe,
                minZoom = minZoom,
                maxZoom = maxZoom,
                doubleTapZoom = doubleTapZoom.coerceIn(minZoom, maxZoom),
                ramCacheSize = ramCacheSize,
                diskCacheSize = diskCacheSize,
                preloadPageCount = preloadPageCount,
            )
        }
    }

    companion object {
        const val DefaultRamCacheSize: Int = 6
        const val DefaultDiskCacheSize: Int = 24

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
