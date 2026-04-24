package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun BindPdfSaveEffect(
    state: KPdfViewerState,
) {
    val saveLauncher = rememberPdfSaveLauncher()
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(state, saveLauncher) {
        state.saveRequests.collectLatest { request ->
            saveLauncher.launch(request) { result ->
                currentState.onSaveResult(
                    requestId = request.requestId,
                    result = result,
                )
            }
        }
    }
}
