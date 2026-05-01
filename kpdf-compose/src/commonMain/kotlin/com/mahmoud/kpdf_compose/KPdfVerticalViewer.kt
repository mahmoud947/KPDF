package com.mahmoud.kpdf_compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfLoadState
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import kotlin.math.roundToInt

/*
 * Created by Mahmoud Kamal El-Din on 01/05/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@Composable
fun KPdfVerticalViewer(
    state: KPdfViewerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    pageSpacing: Dp = 12.dp,
    style: KPdfVerticalViewerStyle = KPdfVerticalViewerStyle.defaults(),
    loadingContent: @Composable () -> Unit = { KPdfDefaultLoadingContent() },
    errorContent: @Composable (message: String) -> Unit = { message ->
        KPdfDefaultMessageContent(
            message = message,
            color = MaterialTheme.colorScheme.error,
        )
    },
) {
    val loadState by state.loadState.collectAsState()
    val currentPageIndex by state.currentPageIndex.collectAsState()
    val pageCount = (loadState as? KPdfLoadState.Ready)?.pageCount.orZero()
    val listState = rememberLazyListState()
    val visiblePageIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val isScrollInProgress by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    LaunchedEffect(pageCount, currentPageIndex) {
        if (pageCount == 0) return@LaunchedEffect
        val targetIndex = currentPageIndex.coerceIn(0, pageCount - 1)
        if (targetIndex != visiblePageIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(pageCount, visiblePageIndex, isScrollInProgress) {
        if (pageCount == 0) return@LaunchedEffect
        if (isScrollInProgress) return@LaunchedEffect
        val targetIndex = visiblePageIndex.coerceIn(0, pageCount - 1)
        if (targetIndex != currentPageIndex) {
            state.goToPage(targetIndex)
        }
    }

    Card(
        modifier = modifier,
        shape = style.containerShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        when (loadState) {
            KPdfLoadState.Idle -> KPdfVerticalMessage("Document is waiting to load.")
            KPdfLoadState.Loading -> loadingContent()
            is KPdfLoadState.Error -> errorContent((loadState as KPdfLoadState.Error).reason.message)

            is KPdfLoadState.Ready -> {
                if (pageCount == 0) {
                    KPdfVerticalMessage("PDF document contains no pages.")
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                    ) {
                        items(
                            count = pageCount,
                            key = { pageIndex -> pageIndex },
                        ) { pageIndex ->
                            KPdfVerticalPage(
                                state = state,
                                config = state.config,
                                pageIndex = pageIndex,
                                isInteractive = pageIndex == visiblePageIndex && !isScrollInProgress,
                                style = style,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (pageIndex == pageCount - 1) 0.dp else pageSpacing),
                                loadingContent = loadingContent,
                                errorContent = errorContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KPdfVerticalPage(
    state: KPdfViewerState,
    config: KPdfViewerConfig,
    pageIndex: Int,
    isInteractive: Boolean,
    style: KPdfVerticalViewerStyle,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (message: String) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val targetWidth = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val targetHeight = remember(targetWidth) {
            (targetWidth * DefaultVerticalPageHeightRatio).roundToInt().coerceAtLeast(1)
        }
        val pageState by produceState<VerticalPageRenderState>(
            initialValue = VerticalPageRenderState.Loading,
            state,
            pageIndex,
            targetWidth,
            targetHeight,
        ) {
            value = VerticalPageRenderState.Loading
            value = state.renderPage(
                pageIndex = pageIndex,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                zoom = 1f,
            ).fold(
                onSuccess = { page -> VerticalPageRenderState.Ready(page) },
                onFailure = { throwable ->
                    VerticalPageRenderState.Error(throwable.message ?: "Unable to render page")
                },
            )
        }

        val pageAspectRatio = (pageState as? VerticalPageRenderState.Ready)
            ?.page
            ?.let { page -> page.width.toFloat() / page.height.toFloat() }
            ?: DefaultVerticalPageAspectRatio

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(pageAspectRatio),
            shape = style.pageShape,
            tonalElevation = style.pageElevation,
            border = BorderStroke(style.pageBorderWidth, style.pageBorderColor),
            color = style.pageContainerColor,
        ) {
            when (val renderedState = pageState) {
                VerticalPageRenderState.Loading -> loadingContent()
                is VerticalPageRenderState.Ready -> {
                    val pageModifier = Modifier
                        .fillMaxSize()
                        .padding(style.pageContentPadding)

                    if (isInteractive) {
                        KVerticalPlatformImageView(
                            page = renderedState.page,
                            state = state,
                            contentDescription = "PDF page ${pageIndex + 1}",
                            config = config,
                            modifier = pageModifier,
                        )
                    } else {
                        KThumbnailImageView(
                            page = renderedState.page,
                            contentDescription = "PDF page ${pageIndex + 1}",
                            modifier = pageModifier,
                        )
                    }
                }

                is VerticalPageRenderState.Error -> errorContent(renderedState.message)
            }
        }
    }
}

@Composable
private fun KPdfVerticalMessage(
    message: String,
) {
    KPdfDefaultMessageContent(message = message)
}

data class KPdfVerticalViewerStyle(
    val containerShape: Shape,
    val pageShape: Shape,
    val pageContainerColor: Color,
    val pageElevation: Dp,
    val pageBorderColor: Color,
    val pageBorderWidth: Dp,
    val pageContentPadding: Dp,
) {
    companion object {
        @Composable
        fun defaults(): KPdfVerticalViewerStyle {
            val colorScheme = MaterialTheme.colorScheme
            return KPdfVerticalViewerStyle(
                containerShape = RoundedCornerShape(24.dp),
                pageShape = RoundedCornerShape(18.dp),
                pageContainerColor = colorScheme.surface,
                pageElevation = 1.dp,
                pageBorderColor = colorScheme.outlineVariant,
                pageBorderWidth = 1.dp,
                pageContentPadding = 8.dp,
            )
        }
    }
}

private sealed interface VerticalPageRenderState {
    data object Loading : VerticalPageRenderState
    data class Ready(val page: KPdfPageBitmap) : VerticalPageRenderState
    data class Error(val message: String) : VerticalPageRenderState
}

private const val DefaultVerticalPageHeightRatio = 1.414f
private const val DefaultVerticalPageAspectRatio = 1f / DefaultVerticalPageHeightRatio

private fun Int?.orZero(): Int = this ?: 0
