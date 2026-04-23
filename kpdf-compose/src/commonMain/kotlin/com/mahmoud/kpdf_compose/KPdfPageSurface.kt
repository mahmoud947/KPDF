package com.mahmoud.kpdf_compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfRenderedPageState
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun KPdfPageSurface(
    state: KPdfViewerState,
    renderedPage: KPdfRenderedPageState,
    config: KPdfViewerConfig,
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

                KPdfZoomablePage(
                    state = state,
                    fallbackPage = page,
                    contentDescription = "PDF page ${page.pageIndex + 1}",
                    pageKey = page.pageIndex,
                    config = config,
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

@Composable
private fun KPdfZoomablePage(
    state: KPdfViewerState,
    fallbackPage: KPdfPageBitmap,
    contentDescription: String?,
    pageKey: Any,
    config: KPdfViewerConfig,
    modifier: Modifier = Modifier,
) {
    var scale by remember(pageKey) { mutableStateOf(config.minZoom) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(pageKey) { mutableStateOf(IntSize.Zero) }
    var displayedPage by remember(pageKey) { mutableStateOf(fallbackPage) }

    LaunchedEffect(fallbackPage) {
        displayedPage = fallbackPage
    }

    fun clampOffset(value: Offset, targetScale: Float = scale): Offset {
        val maxX = viewportSize.width * (targetScale - config.minZoom) / 2f
        val maxY = viewportSize.height * (targetScale - config.minZoom) / 2f

        return Offset(
            x = value.x.coerceOffset(maxX),
            y = value.y.coerceOffset(maxY),
        )
    }

    fun updateScale(targetScale: Float) {
        scale = targetScale.coerceIn(config.minZoom, config.maxZoom)
        offset = clampOffset(offset, scale)
    }

    fun resetZoom() {
        scale = config.minZoom
        offset = Offset.Zero
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(config.minZoom, config.maxZoom)
        scale = nextScale
        offset = if (nextScale > config.minZoom) {
            clampOffset(offset + panChange, nextScale)
        } else {
            Offset.Zero
        }
    }

    val gestureModifier = if (config.enableZoom) {
        Modifier
            .pointerInput(pageKey, config.minZoom, config.doubleTapZoom) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > config.minZoom) {
                            resetZoom()
                        } else {
                            updateScale(config.doubleTapZoom)
                        }
                    },
                )
            }
            .transformable(transformableState)
    } else {
        Modifier
    }
    val requestedRenderScale = quantizeRenderScale(
        scale = if (config.enableZoom) scale else 1f,
    )
    val requestedRenderSize = remember(
        viewportSize,
        requestedRenderScale,
    ) {
        calculateRenderRequest(
            viewportSize = viewportSize,
            renderScale = requestedRenderScale,
        )
    }

    LaunchedEffect(pageKey, requestedRenderSize) {
        if (!requestedRenderSize.isValid()) return@LaunchedEffect
        if (displayedPage.pageIndex == fallbackPage.pageIndex &&
            displayedPage.width >= requestedRenderSize.width &&
            displayedPage.height >= requestedRenderSize.height
        ) {
            return@LaunchedEffect
        }

        if (requestedRenderScale > 1f) {
            delay(RenderUpgradeDelayMillis)
        }

        if (displayedPage.pageIndex == fallbackPage.pageIndex &&
            displayedPage.width >= requestedRenderSize.width &&
            displayedPage.height >= requestedRenderSize.height
        ) {
            return@LaunchedEffect
        }

        state.renderPage(
            pageIndex = fallbackPage.pageIndex,
            targetWidth = requestedRenderSize.width,
            targetHeight = requestedRenderSize.height,
        ).onSuccess { rerenderedPage ->
            if (rerenderedPage.pageIndex == fallbackPage.pageIndex) {
                displayedPage = rerenderedPage
            }
        }
    }

    val activeScale = if (config.enableZoom) scale else 1f
    val activeOffset = if (config.enableZoom) offset else Offset.Zero

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                viewportSize = size
                offset = clampOffset(offset)
            }
            .then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        KPlatformImageView(
            image = displayedPage.image,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = activeScale,
                    scaleY = activeScale,
                    translationX = activeOffset.x,
                    translationY = activeOffset.y,
                ),
        )
    }
}

private fun Float.coerceOffset(maxOffset: Float): Float =
    if (maxOffset > 0f) coerceIn(-maxOffset, maxOffset) else 0f

private data class RenderRequest(
    val width: Int,
    val height: Int,
)

private fun calculateRenderRequest(
    viewportSize: IntSize,
    renderScale: Float,
): RenderRequest {
    if (viewportSize.width <= 0 || viewportSize.height <= 0) {
        return RenderRequest(width = 0, height = 0)
    }

    val rawWidth = (viewportSize.width * renderScale).roundToInt().coerceAtLeast(1)
    val rawHeight = (viewportSize.height * renderScale).roundToInt().coerceAtLeast(1)
    val area = rawWidth.toLong() * rawHeight.toLong()

    if (area <= MaxRenderPixels) {
        return RenderRequest(rawWidth, rawHeight)
    }

    val resizeRatio = sqrt(MaxRenderPixels.toDouble() / area.toDouble()).toFloat()
    return RenderRequest(
        width = (rawWidth * resizeRatio).roundToInt().coerceAtLeast(1),
        height = (rawHeight * resizeRatio).roundToInt().coerceAtLeast(1),
    )
}

private fun RenderRequest.isValid(): Boolean = width > 0 && height > 0

private fun quantizeRenderScale(scale: Float): Float {
    val normalizedScale = scale.coerceAtLeast(1f)
    if (normalizedScale <= 1f) return 1f

    val steps = floor((normalizedScale - 1f) / RenderScaleStep).toInt()
    return 1f + ((steps + 1) * RenderScaleStep)
}

private const val RenderUpgradeDelayMillis = 120L
private const val RenderScaleStep = 0.5f
private const val MaxRenderPixels: Long = 8_388_608L
