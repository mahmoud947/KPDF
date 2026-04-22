package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

sealed interface KPdfRenderedPageState {
    public data object Idle : KPdfRenderedPageState
    public data object Loading : KPdfRenderedPageState

    public data class Ready(
        public val page: KPdfPageBitmap,
    ) : KPdfRenderedPageState

    public data class Error(
        public val reason: KPdfError,
    ) : KPdfRenderedPageState
}