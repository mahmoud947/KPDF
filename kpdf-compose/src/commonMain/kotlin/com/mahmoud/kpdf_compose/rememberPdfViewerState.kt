package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mahmoud.kpdf_core.DefaultKPdf
import com.mahmoud.kpdf_core.api.KPdf
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState

@Composable
public fun rememberPdfViewerState(
    source: KPdfSource,
    config: KPdfViewerConfig = KPdfViewerConfig.builder().build(),
    sdk: KPdf = DefaultKPdf()
): KPdfViewerState {
    val state = remember(source, config, sdk) {
        sdk.viewerState(source = source, config = config)
    }

    LaunchedEffect(state, source) {
        state.open(source)
    }
    DisposableEffect(state) {
        onDispose {
            state.close()
        }
    }

    return state
}