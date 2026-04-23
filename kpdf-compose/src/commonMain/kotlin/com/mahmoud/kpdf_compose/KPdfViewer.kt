package com.mahmoud.kpdf_compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfViewerState

/*
 * Compose Multiplatform PDF viewer shell.
 * Displays the current rendered page and delegates all document behavior to
 * [KPdfViewerState].
 *
 *
 * Created by Mahmoud Kamal El-Din on 23/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */


@Composable
fun KPdfViewer(
    state: KPdfViewerState,
    modifier: Modifier = Modifier,
) {
    val renderedPage by state.renderedPage.collectAsState()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        KPdfContent(
            state = state,
            renderedPage = renderedPage,
            config = state.config,
            modifier = Modifier.fillMaxSize(),
        )
    }

}
