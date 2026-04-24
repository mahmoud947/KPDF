package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun BindPdfOpenEffect(
    state: KPdfViewerState,
) {
    val openLauncher = rememberPdfOpenLauncher()
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(state, openLauncher) {
        state.openDocumentRequests.collectLatest { request ->
            openLauncher.launch(request) { result ->
                currentState.onOpenFromDeviceResult(
                    requestId = request.requestId,
                    result = result,
                )
            }
        }
    }
}
