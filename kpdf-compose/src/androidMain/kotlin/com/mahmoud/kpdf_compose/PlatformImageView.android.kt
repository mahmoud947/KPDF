package com.mahmoud.kpdf_compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@Composable
internal actual fun KPlatformImageView(
    page: KPdfPageBitmap,
    state: KPdfViewerState,
    contentDescription: String?,
    config: KPdfViewerConfig,
    modifier: Modifier
) {
    var scale by remember(page.pageIndex) { mutableStateOf(config.minZoom) }
    var offset by remember(page.pageIndex) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(page.pageIndex) { mutableStateOf(IntSize.Zero) }
    var displayedPage by remember(page.pageIndex) { mutableStateOf(page) }

    LaunchedEffect(page) {
        displayedPage = page
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
            .pointerInput(page.pageIndex, config.minZoom, config.doubleTapZoom) {
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
        page.width,
        page.height,
        viewportSize,
        requestedRenderScale,
    ) {
        calculateRenderTargetSize(
            contentWidth = page.width,
            contentHeight = page.height,
            viewportSize = viewportSize,
            zoom = requestedRenderScale,
        )
    }

    LaunchedEffect(page.pageIndex, requestedRenderSize) {
        if (!requestedRenderSize.isValid()) return@LaunchedEffect
        if (displayedPage.pageIndex == page.pageIndex &&
            displayedPage.width >= requestedRenderSize.width &&
            displayedPage.height >= requestedRenderSize.height
        ) {
            return@LaunchedEffect
        }

        if (requestedRenderScale > 1f) {
            delay(RenderUpgradeDelayMillis)
        }

        if (displayedPage.pageIndex == page.pageIndex &&
            displayedPage.width >= requestedRenderSize.width &&
            displayedPage.height >= requestedRenderSize.height
        ) {
            return@LaunchedEffect
        }

        state.renderPage(
            pageIndex = page.pageIndex,
            targetWidth = viewportSize.width.coerceAtLeast(1),
            targetHeight = viewportSize.height.coerceAtLeast(1),
            zoom = requestedRenderScale,
        ).onSuccess { rerenderedPage ->
            if (rerenderedPage.pageIndex == page.pageIndex) {
                displayedPage = rerenderedPage
            }
        }
    }

    val bitmap = displayedPage.image.bitmap ?: return
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
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = activeScale,
                    scaleY = activeScale,
                    translationX = activeOffset.x,
                    translationY = activeOffset.y,
                ),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
        )
    }
}

private fun Float.coerceOffset(maxOffset: Float): Float =
    if (maxOffset > 0f) coerceIn(-maxOffset, maxOffset) else 0f

private data class RenderRequest(
    val width: Int,
    val height: Int,
)

private fun calculateRenderTargetSize(
    contentWidth: Int,
    contentHeight: Int,
    viewportSize: IntSize,
    zoom: Float,
): RenderRequest {
    if (viewportSize.width <= 0 || viewportSize.height <= 0) {
        return RenderRequest(width = 0, height = 0)
    }

    val safeContentWidth = contentWidth.coerceAtLeast(1)
    val safeContentHeight = contentHeight.coerceAtLeast(1)
    val contentAspect = safeContentWidth.toFloat() / safeContentHeight.toFloat()
    val viewportAspect = viewportSize.width.toFloat() / viewportSize.height.toFloat()
    val fitted = if (contentAspect > viewportAspect) {
        RenderRequest(
            width = viewportSize.width,
            height = (viewportSize.width / contentAspect).roundToInt().coerceAtLeast(1),
        )
    } else {
        RenderRequest(
            width = (viewportSize.height * contentAspect).roundToInt().coerceAtLeast(1),
            height = viewportSize.height,
        )
    }

    val rawWidth = (fitted.width * zoom).roundToInt().coerceAtLeast(1)
    val rawHeight = (fitted.height * zoom).roundToInt().coerceAtLeast(1)
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
