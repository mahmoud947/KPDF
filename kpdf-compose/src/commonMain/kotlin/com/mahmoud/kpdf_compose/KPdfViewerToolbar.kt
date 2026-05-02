package com.mahmoud.kpdf_compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_core.api.KPdfLoadState
import com.mahmoud.kpdf_core.api.KPdfSearchState
import com.mahmoud.kpdf_core.api.KPdfViewerState

typealias KPdfToolbarIcon = @Composable (() -> Unit)

@Composable
fun KPdfViewerToolbar(
    state: KPdfViewerState,
    isThumbnailStripVisible: Boolean,
    onThumbnailToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    onZoomInClick: (() -> Unit)? = { state.zoomIn() },
    onZoomOutClick: (() -> Unit)? = { state.zoomOut() },
    onSearchClick: (() -> Unit)? = null,
    onPreviousSearchResultClick: (() -> Unit)? = { state.previousSearchResult() },
    onNextSearchResultClick: (() -> Unit)? = { state.nextSearchResult() },
    onClearSearchClick: (() -> Unit)? = { state.clearSearch() },
    onSaveClick: (() -> Unit)? = { state.requestSave() },
    onShareClick: (() -> Unit)? = null,
    config: KPdfViewerToolbarConfig = KPdfViewerToolbarConfig.defaults(),
    style: KPdfViewerToolbarStyle = KPdfViewerToolbarStyle.defaults(),
) {
    val loadState by state.loadState.collectAsState()
    val currentPageIndex by state.currentPageIndex.collectAsState()
    val currentZoom by state.currentZoom.collectAsState()
    val searchState by state.searchState.collectAsState()
    val pageCount = (loadState as? KPdfLoadState.Ready)?.pageCount ?: 0
    val currentPageNumber = if (pageCount == 0) 0 else currentPageIndex + 1
    val zoomInEnabled = currentZoom < state.config.maxZoom
    val zoomOutEnabled = currentZoom > state.config.minZoom
    val zoomPercentage = (currentZoom * 100).toInt()
    val searchResultCount = (searchState as? KPdfSearchState.Success)?.results?.size ?: 0
    val activeSearchResultNumber = (searchState as? KPdfSearchState.Success)
        ?.activeResultIndex
        ?.takeIf { it >= 0 }
        ?.plus(1)
        ?: 0
    val hasSearchState = searchState !is KPdfSearchState.Idle
    val searchNavigationEnabled = searchResultCount > 1
    var localSearchQuery by remember(searchQuery) { mutableStateOf(searchQuery) }
    var searchFieldVisible by remember(searchQuery) {
        mutableStateOf(searchQuery.isNotBlank())
    }
    val currentSearchQuery = if (onSearchQueryChange != null) {
        searchQuery
    } else {
        localSearchQuery
    }
    val performSearch: (() -> Unit) = onSearchClick ?: {
        state.searchText(currentSearchQuery)
    }
    val searchEnabled = onSearchClick != null || currentSearchQuery.isNotBlank()
    val searchClick: () -> Unit = {
        if (searchFieldVisible) {
            if (searchEnabled) {
                performSearch()
            }
        } else {
            searchFieldVisible = true
        }
    }
    val clearSearchClick: (() -> Unit)? = onClearSearchClick?.let { clearSearch ->
        {
            clearSearch()
            if (onSearchQueryChange != null) {
                onSearchQueryChange("")
            } else {
                localSearchQuery = ""
            }
            searchFieldVisible = false
        }
    }

    Row(
        modifier = modifier
            .clip(style.containerShape)
            .background(
                brush = Brush.horizontalGradient(style.containerGradientColors),
                shape = style.containerShape,
            )
            .border(
                width = style.containerBorderWidth,
                color = style.containerBorderColor,
                shape = style.containerShape,
            )
            .fillMaxWidth()
            .padding(style.contentPadding)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
    ) {
        if (config.visibility.showPageSummary) {
            ToolbarChip(
                text = config.strings.pageSummaryText(currentPageNumber, pageCount),
                leadingIcon = config.icons.pageSummaryIcon,
                onClick = null,
                style = style,
                appearance = KPdfToolbarChipAppearance.Accent,
            )
        }

        if (config.visibility.showSearch) {
            if (searchFieldVisible) {
                OutlinedTextField(
                    value = currentSearchQuery,
                    onValueChange = { query ->
                        if (onSearchQueryChange != null) {
                            onSearchQueryChange(query)
                        } else {
                            localSearchQuery = query
                        }
                    },
                    modifier = Modifier.widthIn(
                        min = style.searchFieldMinWidth,
                        max = style.searchFieldMaxWidth,
                    ),
                    singleLine = true,
                    placeholder = {
                        Text(config.strings.searchPlaceholderText)
                    },
                    leadingIcon = config.icons.searchIcon,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchEnabled) {
                                performSearch()
                            }
                        },
                    ),
                    shape = style.chipShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = style.chipColor,
                        unfocusedContainerColor = style.chipColor,
                        disabledContainerColor = style.disabledChipColor,
                        focusedBorderColor = style.accentChipBorderColor,
                        unfocusedBorderColor = style.chipBorderColor,
                        focusedTextColor = style.chipContentColor,
                        unfocusedTextColor = style.chipContentColor,
                        focusedPlaceholderColor = style.disabledChipContentColor,
                        unfocusedPlaceholderColor = style.disabledChipContentColor,
                        focusedLeadingIconColor = style.chipContentColor,
                        unfocusedLeadingIconColor = style.chipContentColor,
                    ),
                )
            }
            ToolbarChip(
                text = config.strings.searchText,
                leadingIcon = if (searchFieldVisible) null else config.icons.searchIcon,
                onClick = searchClick,
                enabled = !searchFieldVisible || searchEnabled,
                style = style,
                appearance = if (hasSearchState) {
                    KPdfToolbarChipAppearance.Accent
                } else {
                    KPdfToolbarChipAppearance.Default
                },
            )
        }

        if (config.visibility.showSearchSummary && hasSearchState) {
            ToolbarChip(
                text = config.strings.searchSummaryText(searchState),
                leadingIcon = config.icons.searchSummaryIcon,
                onClick = null,
                style = style,
                appearance = KPdfToolbarChipAppearance.Tonal,
            )
        }

        if (config.visibility.showSearchResultNavigation && hasSearchState) {
            ToolbarChip(
                text = config.strings.previousSearchResultText,
                leadingIcon = config.icons.previousSearchResultIcon,
                onClick = onPreviousSearchResultClick,
                enabled = searchNavigationEnabled,
                style = style,
            )
            ToolbarChip(
                text = config.strings.nextSearchResultText,
                leadingIcon = config.icons.nextSearchResultIcon,
                onClick = onNextSearchResultClick,
                enabled = searchNavigationEnabled,
                style = style,
            )
        }

        if (config.visibility.showClearSearch && hasSearchState) {
            ToolbarChip(
                text = config.strings.clearSearchText(
                    activeSearchResultNumber,
                    searchResultCount,
                ),
                leadingIcon = config.icons.clearSearchIcon,
                onClick = clearSearchClick,
                style = style,
            )
        }

        if (config.visibility.showZoomOut) {
            ToolbarChip(
                text = config.strings.zoomOutText,
                leadingIcon = config.icons.zoomOutIcon,
                onClick = onZoomOutClick,
                enabled = zoomOutEnabled,
                style = style,
            )
        }

        if (config.visibility.showZoomPercentage) {
            ToolbarChip(
                text = config.strings.zoomPercentageText(zoomPercentage),
                leadingIcon = config.icons.zoomPercentageIcon,
                onClick = null,
                style = style,
                appearance = KPdfToolbarChipAppearance.Tonal,
            )
        }

        if (config.visibility.showZoomIn) {
            ToolbarChip(
                text = config.strings.zoomInText,
                leadingIcon = config.icons.zoomInIcon,
                onClick = onZoomInClick,
                enabled = zoomInEnabled,
                style = style,
            )
        }

        if (config.visibility.showSave) {
            ToolbarChip(
                text = config.strings.saveText,
                leadingIcon = config.icons.saveIcon,
                onClick = onSaveClick,
                style = style,
            )
        }

        if (config.visibility.showShare) {
            ToolbarChip(
                text = config.strings.shareText,
                leadingIcon = config.icons.shareIcon,
                onClick = onShareClick,
                style = style,
            )
        }

        if (config.visibility.showThumbnailToggle) {
            ToolbarChip(
                text = config.strings.thumbnailToggleText(isThumbnailStripVisible),
                leadingIcon = if (isThumbnailStripVisible) {
                    config.icons.thumbnailVisibleIcon
                } else {
                    config.icons.thumbnailHiddenIcon
                },
                onClick = { onThumbnailToggle(!isThumbnailStripVisible) },
                style = style,
                appearance = if (isThumbnailStripVisible) {
                    KPdfToolbarChipAppearance.Accent
                } else {
                    KPdfToolbarChipAppearance.Default
                },
            )
        }
    }
}

@Composable
private fun ToolbarChip(
    text: String,
    style: KPdfViewerToolbarStyle,
    onClick: (() -> Unit)?,
    leadingIcon: KPdfToolbarIcon? = null,
    enabled: Boolean = true,
    appearance: KPdfToolbarChipAppearance = KPdfToolbarChipAppearance.Default,
) {
    val effectiveEnabled = enabled && onClick != null
    val containerColor = when (appearance) {
        KPdfToolbarChipAppearance.Default -> style.chipColor
        KPdfToolbarChipAppearance.Tonal -> style.tonalChipColor
        KPdfToolbarChipAppearance.Accent -> style.accentChipColor
    }
    val borderColor = when (appearance) {
        KPdfToolbarChipAppearance.Default -> style.chipBorderColor
        KPdfToolbarChipAppearance.Tonal -> style.tonalChipBorderColor
        KPdfToolbarChipAppearance.Accent -> style.accentChipBorderColor
    }
    val contentColor = when (appearance) {
        KPdfToolbarChipAppearance.Default -> style.chipContentColor
        KPdfToolbarChipAppearance.Tonal -> style.tonalChipContentColor
        KPdfToolbarChipAppearance.Accent -> style.accentChipContentColor
    }

    AssistChip(
        onClick = { onClick?.invoke() },
        enabled = effectiveEnabled,
        label = {
            Text(
                text = text,
                fontWeight = if (appearance == KPdfToolbarChipAppearance.Accent) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
            )
        },
        leadingIcon = leadingIcon,
        modifier = Modifier.widthIn(min = style.minChipWidth),
        shape = style.chipShape,
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = borderColor,
            disabledBorderColor = style.disabledChipBorderColor,
            borderWidth = style.chipBorderWidth,
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor,
            disabledContainerColor = style.disabledChipColor,
            disabledLabelColor = style.disabledChipContentColor,
            disabledLeadingIconContentColor = style.disabledChipContentColor,
        ),
    )
}

enum class KPdfToolbarChipAppearance {
    Default,
    Tonal,
    Accent,
}

data class KPdfViewerToolbarConfig(
    val visibility: KPdfViewerToolbarVisibility,
    val strings: KPdfViewerToolbarStrings,
    val icons: KPdfViewerToolbarIcons,
) {
    companion object {
        @Composable
        fun defaults(): KPdfViewerToolbarConfig = KPdfViewerToolbarConfig(
            visibility = KPdfViewerToolbarVisibility(),
            strings = KPdfViewerToolbarStrings.defaults(),
            icons = KPdfViewerToolbarIcons.defaults(),
        )
    }
}

data class KPdfViewerToolbarVisibility(
    val showPageSummary: Boolean = true,
    val showSearch: Boolean = true,
    val showSearchSummary: Boolean = true,
    val showSearchResultNavigation: Boolean = true,
    val showClearSearch: Boolean = true,
    val showZoomOut: Boolean = true,
    val showZoomPercentage: Boolean = true,
    val showZoomIn: Boolean = true,
    val showSave: Boolean = true,
    val showShare: Boolean = true,
    val showThumbnailToggle: Boolean = true,
)

data class KPdfViewerToolbarStrings(
    val pageSummaryText: (currentPage: Int, pageCount: Int) -> String,
    val searchSummaryText: (state: KPdfSearchState) -> String,
    val zoomPercentageText: (zoomPercent: Int) -> String,
    val searchPlaceholderText: String,
    val searchText: String,
    val previousSearchResultText: String,
    val nextSearchResultText: String,
    val clearSearchText: (currentResult: Int, resultCount: Int) -> String,
    val zoomOutText: String,
    val zoomInText: String,
    val saveText: String,
    val shareText: String,
    val thumbnailToggleText: (isVisible: Boolean) -> String,
) {
    companion object {
        fun defaults(): KPdfViewerToolbarStrings = KPdfViewerToolbarStrings(
            pageSummaryText = { currentPage, pageCount ->
                if (pageCount == 0) "Page --" else "Page $currentPage / $pageCount"
            },
            searchSummaryText = { state ->
                when (state) {
                    KPdfSearchState.Idle -> "Search"
                    is KPdfSearchState.Searching -> "Searching"
                    is KPdfSearchState.Success -> {
                        val current = state.activeResultIndex
                            .takeIf { it >= 0 }
                            ?.plus(1)
                            ?: 0
                        "$current / ${state.results.size}"
                    }
                    is KPdfSearchState.Error -> "Search Failed"
                }
            },
            zoomPercentageText = { zoomPercent -> "$zoomPercent%" },
            searchPlaceholderText = "Find in PDF",
            searchText = "Search",
            previousSearchResultText = "Prev Match",
            nextSearchResultText = "Next Match",
            clearSearchText = { _, _ -> "Clear" },
            zoomOutText = "Zoom Out",
            zoomInText = "Zoom In",
            saveText = "Save",
            shareText = "Share",
            thumbnailToggleText = { isVisible ->
                if (isVisible) "Hide Thumbnails" else "Show Thumbnails"
            },
        )
    }
}

data class KPdfViewerToolbarIcons(
    val pageSummaryIcon: KPdfToolbarIcon? = null,
    val searchIcon: KPdfToolbarIcon? = null,
    val searchSummaryIcon: KPdfToolbarIcon? = null,
    val previousSearchResultIcon: KPdfToolbarIcon? = null,
    val nextSearchResultIcon: KPdfToolbarIcon? = null,
    val clearSearchIcon: KPdfToolbarIcon? = null,
    val zoomPercentageIcon: KPdfToolbarIcon? = null,
    val zoomOutIcon: KPdfToolbarIcon? = null,
    val zoomInIcon: KPdfToolbarIcon? = null,
    val saveIcon: KPdfToolbarIcon? = null,
    val shareIcon: KPdfToolbarIcon? = null,
    val thumbnailVisibleIcon: KPdfToolbarIcon? = null,
    val thumbnailHiddenIcon: KPdfToolbarIcon? = null,
) {
    companion object {
        @Composable
        fun defaults(): KPdfViewerToolbarIcons = KPdfViewerToolbarIcons(
            pageSummaryIcon = { ToolbarGlyph("Pg") },
            searchIcon = { ToolbarGlyph("F") },
            searchSummaryIcon = { ToolbarGlyph("#") },
            previousSearchResultIcon = { ToolbarGlyph("<") },
            nextSearchResultIcon = { ToolbarGlyph(">") },
            clearSearchIcon = { ToolbarGlyph("X") },
            zoomPercentageIcon = { ToolbarGlyph("%") },
            zoomOutIcon = { ToolbarGlyph("-") },
            zoomInIcon = { ToolbarGlyph("+") },
            saveIcon = { ToolbarGlyph("S") },
            shareIcon = { ToolbarGlyph("Sh") },
            thumbnailVisibleIcon = { ToolbarGlyph("T") },
            thumbnailHiddenIcon = { ToolbarGlyph("T") },
        )
    }
}

data class KPdfViewerToolbarStyle(
    val containerShape: Shape,
    val containerGradientColors: List<Color>,
    val containerBorderColor: Color,
    val containerBorderWidth: Dp,
    val chipShape: Shape,
    val chipColor: Color,
    val tonalChipColor: Color,
    val accentChipColor: Color,
    val disabledChipColor: Color,
    val chipBorderColor: Color,
    val tonalChipBorderColor: Color,
    val accentChipBorderColor: Color,
    val disabledChipBorderColor: Color,
    val chipBorderWidth: Dp,
    val chipContentColor: Color,
    val tonalChipContentColor: Color,
    val accentChipContentColor: Color,
    val disabledChipContentColor: Color,
    val contentPadding: Dp,
    val itemSpacing: Dp,
    val minChipWidth: Dp,
    val searchFieldMinWidth: Dp,
    val searchFieldMaxWidth: Dp,
) {
    companion object {
        @Composable
        fun defaults(): KPdfViewerToolbarStyle {
            val colorScheme = MaterialTheme.colorScheme
            return KPdfViewerToolbarStyle(
                containerShape = RoundedCornerShape(24.dp),
                containerGradientColors = listOf(
                    colorScheme.surface.copy(alpha = 0.98f),
                    colorScheme.surfaceContainerLowest.copy(alpha = 0.98f),
                    colorScheme.surfaceVariant.copy(alpha = 0.94f),
                ),
                containerBorderColor = colorScheme.outlineVariant.copy(alpha = 0.8f),
                containerBorderWidth = 1.dp,
                chipShape = RoundedCornerShape(18.dp),
                chipColor = colorScheme.surface.copy(alpha = 0.92f),
                tonalChipColor = colorScheme.surfaceVariant.copy(alpha = 0.82f),
                accentChipColor = colorScheme.primaryContainer.copy(alpha = 0.92f),
                disabledChipColor = colorScheme.surfaceVariant.copy(alpha = 0.55f),
                chipBorderColor = colorScheme.outlineVariant.copy(alpha = 0.85f),
                tonalChipBorderColor = colorScheme.outline.copy(alpha = 0.7f),
                accentChipBorderColor = colorScheme.outline.copy(alpha = 0.9f),
                disabledChipBorderColor = colorScheme.outline.copy(alpha = 0.28f),
                chipBorderWidth = 1.dp,
                chipContentColor = colorScheme.onSurface,
                tonalChipContentColor = colorScheme.onSurface,
                accentChipContentColor = colorScheme.onPrimaryContainer,
                disabledChipContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                contentPadding = 14.dp,
                itemSpacing = 10.dp,
                minChipWidth = 88.dp,
                searchFieldMinWidth = 180.dp,
                searchFieldMaxWidth = 260.dp,
            )
        }
    }
}

@Composable
private fun ToolbarGlyph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}
