package com.mahmoud.kpdf_compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState

@Composable
internal fun KPdfPageSurface(
    state: KPdfViewerState,
    renderedPage: KPdfRenderedPageState,
    config: KPdfViewerConfig,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (message: String) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (renderedPage) {
            KPdfRenderedPageState.Idle -> {
                Text("Page is waiting to render.")
            }

            KPdfRenderedPageState.Loading -> {
                loadingContent()
            }

            is KPdfRenderedPageState.Ready -> {
                val page = renderedPage.page

                KPlatformImageView(
                    page = page,
                    state = state,
                    contentDescription = "PDF page ${page.pageIndex + 1}",
                    config = config,
                    modifier = Modifier.fillMaxSize(),
                )

            }

            is KPdfRenderedPageState.Error -> {
                errorContent(renderedPage.reason.message)
            }
        }
    }
}

@Composable
fun KPdfDefaultLoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun KPdfDefaultMessageContent(
    message: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
