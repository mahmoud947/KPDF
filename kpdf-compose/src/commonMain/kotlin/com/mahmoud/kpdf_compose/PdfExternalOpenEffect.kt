package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun BindPdfExternalOpenEffect(
    state: KPdfViewerState,
) {
    val externalOpenLauncher = rememberPdfExternalOpenLauncher()
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(state, externalOpenLauncher) {
        state.externalOpenRequests.collectLatest { request ->
            externalOpenLauncher.launch(request) { result ->
                currentState.onExternalOpenResult(
                    requestId = request.requestId,
                    result = result,
                )
            }
        }
    }
}
