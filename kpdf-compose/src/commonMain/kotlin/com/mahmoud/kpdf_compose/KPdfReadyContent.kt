package com.mahmoud.kpdf_compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import com.mahmoud.kpdf_core.api.KPdfViewerConfig

@Composable
internal fun KPdfReadyContent(
    renderedPage: KPdfRenderedPageState,
    config: KPdfViewerConfig,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KPdfPageSurface(
            renderedPage = renderedPage,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}