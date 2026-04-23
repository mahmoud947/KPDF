package com.mahmoud.kpdf_compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import kotlinx.coroutines.FlowPreview

@Composable
internal fun KPdfPageSurface(
    renderedPage: KPdfRenderedPageState,
    modifier: Modifier = Modifier,
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
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                )
            }

            is KPdfRenderedPageState.Ready -> {
                val page = renderedPage.page

                KPlatformImageView(
                    image = page.image,
                    contentDescription = "PDF page ${page.pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                )

            }

            is KPdfRenderedPageState.Error -> {
                Text(
                    text = renderedPage.reason.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}