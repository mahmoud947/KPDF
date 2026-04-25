package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mahmoud.kpdf_core.api.KPdfFactory
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@Composable
fun rememberPdfViewerState(
    source: KPdfSource,
    config: KPdfViewerConfig = KPdfViewerConfig.builder().build(),
): KPdfViewerState {

    val sdk = remember { KPdfFactory.create() }

    val state = remember(source, config, sdk) {
        sdk.viewerState(source = source, config = config)
    }

    LaunchedEffect(state, source) {
        state.open(source)
    }
    BindPdfOpenEffect(state)
    BindPdfSaveEffect(state)
    BindPdfExternalOpenEffect(state)

    DisposableEffect(state) {
        onDispose {
            state.close()
        }
    }

    return state
}
