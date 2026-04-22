package com.mahmoud.kpdf_core.api

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
sealed interface KPdfLoadState {
    data object Idle : KPdfLoadState
    data object Loading : KPdfLoadState

    data class Ready(
        val pageCount: Int,
        val title: String? = null,
    ) : KPdfLoadState

    data class Error(
        val reason: KPdfError,
    ) : KPdfLoadState
}
