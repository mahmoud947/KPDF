package com.mahmoud.kpdf_compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfLoadState
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfViewerState

@Composable
fun KPdfThumbnailStrip(
    state: KPdfViewerState,
    modifier: Modifier = Modifier,
    onPageClick: ((Int) -> Unit)? = { pageIndex -> state.goToPage(pageIndex) },
    thumbnailWidth: Dp = 88.dp,
    thumbnailHeight: Dp = 124.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    itemSpacing: Dp = 12.dp,
    style: KPdfThumbnailStripStyle = KPdfThumbnailStripStyle.defaults(),
) {
    val loadState by state.loadState.collectAsState()
    val currentPageIndex by state.currentPageIndex.collectAsState()
    val pageCount = (loadState as? KPdfLoadState.Ready)?.pageCount.orZero()
    val listState = rememberLazyListState()

    LaunchedEffect(pageCount, currentPageIndex) {
        if (pageCount == 0) return@LaunchedEffect
        listState.animateScrollToItem(index = currentPageIndex.coerceIn(0, pageCount - 1))
    }

    Surface(
        modifier = modifier,
        shape = style.stripShape,
        color = style.stripContainerColor,
    ) {
        if (pageCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbnailHeight + 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No pages",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                contentPadding = contentPadding,
            ) {
                items(
                    items = List(pageCount) { it },
                    key = { pageIndex -> pageIndex },
                ) { pageIndex ->
                    KPdfThumbnailItem(
                        state = state,
                        pageIndex = pageIndex,
                        isSelected = pageIndex == currentPageIndex,
                        onPageClick = onPageClick,
                        thumbnailWidth = thumbnailWidth,
                        thumbnailHeight = thumbnailHeight,
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun KPdfThumbnailItem(
    state: KPdfViewerState,
    pageIndex: Int,
    isSelected: Boolean,
    onPageClick: ((Int) -> Unit)?,
    thumbnailWidth: Dp,
    thumbnailHeight: Dp,
    style: KPdfThumbnailStripStyle,
) {
    val density = LocalDensity.current
    val targetWidth = remember(thumbnailWidth, density) {
        with(density) { thumbnailWidth.roundToPx().coerceAtLeast(1) }
    }
    val targetHeight = remember(thumbnailHeight, density) {
        with(density) { thumbnailHeight.roundToPx().coerceAtLeast(1) }
    }
    var renderVersion by remember(pageIndex, targetWidth, targetHeight) { mutableStateOf(0) }
    val thumbnailState by produceState<ThumbnailRenderState>(
        initialValue = ThumbnailRenderState.Loading,
        state,
        pageIndex,
        targetWidth,
        targetHeight,
        renderVersion,
    ) {
        value = ThumbnailRenderState.Loading
        value = state.renderPage(
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = 1f,
        ).fold(
            onSuccess = { page -> ThumbnailRenderState.Ready(page) },
            onFailure = { throwable -> ThumbnailRenderState.Error(throwable.message ?: "Unable to render page") },
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val isClickable = onPageClick != null
        val itemModifier = Modifier
            .width(thumbnailWidth)
            .height(thumbnailHeight)
            .then(
                if (isClickable) {
                    Modifier.clickable { onPageClick.invoke(pageIndex) }
                } else {
                    Modifier
                }
            )
        Surface(
            modifier = itemModifier,
            shape = style.thumbnailShape,
            tonalElevation = if (isSelected) style.selectedThumbnailElevation else style.thumbnailElevation,
            border = BorderStroke(
                width = if (isSelected) style.selectedBorderWidth else style.borderWidth,
                color = if (isSelected) {
                    style.selectedBorderColor
                } else {
                    style.borderColor
                },
            ),
            color = if (isSelected) style.selectedThumbnailContainerColor else style.thumbnailContainerColor,
        ) {
            when (val pageState = thumbnailState) {
                ThumbnailRenderState.Loading -> {
                    Box(
                        modifier = Modifier
                            .background(style.loadingContainerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                is ThumbnailRenderState.Ready -> {
                    KThumbnailImageView(
                        page = pageState.page,
                        contentDescription = "Thumbnail for page ${pageIndex + 1}",
                        modifier = Modifier.padding(style.thumbnailContentPadding),
                    )
                }

                is ThumbnailRenderState.Error -> {
                    Box(
                        modifier = Modifier
                            .background(style.errorContainerColor)
                            .then(
                                if (isClickable) {
                                    Modifier.clickable { renderVersion++ }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = pageState.message,
                            modifier = Modifier.padding(8.dp),
                            color = style.errorContentColor,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                        )
                    }
                }
            }
        }

        Text(
            text = "${pageIndex + 1}",
            color = if (isSelected) {
                style.selectedPageNumberColor
            } else {
                style.pageNumberColor
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private sealed interface ThumbnailRenderState {
    data object Loading : ThumbnailRenderState
    data class Ready(val page: KPdfPageBitmap) : ThumbnailRenderState
    data class Error(val message: String) : ThumbnailRenderState
}

data class KPdfThumbnailStripStyle(
    val stripShape: Shape,
    val stripContainerColor: Color,
    val thumbnailShape: Shape,
    val thumbnailContainerColor: Color,
    val selectedThumbnailContainerColor: Color,
    val thumbnailElevation: Dp,
    val selectedThumbnailElevation: Dp,
    val borderColor: Color,
    val selectedBorderColor: Color,
    val borderWidth: Dp,
    val selectedBorderWidth: Dp,
    val thumbnailContentPadding: Dp,
    val loadingContainerColor: Color,
    val errorContainerColor: Color,
    val errorContentColor: Color,
    val pageNumberColor: Color,
    val selectedPageNumberColor: Color,
) {
    companion object {
        @Composable
        fun defaults(
            colorScheme: ColorScheme = MaterialTheme.colorScheme,
        ): KPdfThumbnailStripStyle = KPdfThumbnailStripStyle(
            stripShape = RoundedCornerShape(18.dp),
            stripContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.32f),
            thumbnailShape = RoundedCornerShape(16.dp),
            thumbnailContainerColor = colorScheme.surface,
            selectedThumbnailContainerColor = colorScheme.surface,
            thumbnailElevation = 0.dp,
            selectedThumbnailElevation = 2.dp,
            borderColor = colorScheme.outlineVariant,
            selectedBorderColor = colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp,
            thumbnailContentPadding = 6.dp,
            loadingContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.42f),
            errorContainerColor = colorScheme.errorContainer.copy(alpha = 0.5f),
            errorContentColor = colorScheme.onErrorContainer,
            pageNumberColor = colorScheme.onSurfaceVariant,
            selectedPageNumberColor = colorScheme.primary,
        )
    }
}

private fun Int?.orZero(): Int = this ?: 0
