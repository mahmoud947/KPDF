# KPDF Library Guide

KPDF is a Kotlin Multiplatform PDF library for Android and iOS with a Compose Multiplatform viewer layer.

This document focuses on how to integrate and use the library in real applications.

## Modules

- `kpdf-core`
  Shared PDF source loading, rendering, caching, navigation, zoom, save/export, external-open, and picker state APIs.
- `kpdf-compose`
  Compose Multiplatform UI components and platform integration for save/open flows.
- `composeApp`
  Sample application that demonstrates how the library is intended to be used.

## What You Use Most

- `KPdfSource`
  Describes where the PDF comes from.
- `KPdfViewerConfig`
  Controls viewer behavior such as zoom range, cache sizes, and preloading.
- `KPdfViewerState`
  The main state holder and controller for a single viewer instance.
- `KPdfViewer`
  The main page surface.
- `KPdfThumbnailStrip`
  An optional connected secondary view that shares the same viewer state.
- `KPdfViewerToolbar`
  An optional connected toolbar view that also shares the same viewer state.

## Supported PDF Sources

KPDF currently supports:

- `KPdfSource.Url`
- `KPdfSource.Bytes`
- `KPdfSource.Base64`

Examples:

```kotlin
val urlSource = KPdfSource.Url(
    url = "https://example.com/document.pdf",
    headers = mapOf("Authorization" to "Bearer token")
)

val bytesSource = KPdfSource.Bytes(pdfBytes)

val base64Source = KPdfSource.Base64(base64String)
```

## 1. Create Viewer State

In Compose, the easiest entry point is `rememberPdfViewerState`.

```kotlin
@Composable
fun PdfScreen(source: KPdfSource) {
    val stableSource = remember(source) { source }
    val viewerConfig = remember {
        KPdfViewerConfig.builder()
            .zoomRange(minZoom = 1f, maxZoom = 5f)
            .doubleTapZoom(2f)
            .ramCacheSize(6)
            .diskCacheSize(24)
            .preloadPageCount(2)
            .build()
    }

    val viewerState = rememberPdfViewerState(
        source = stableSource,
        config = viewerConfig,
    )

    KPdfViewer(state = viewerState)
}
```

Keep `source` and `config` stable in Compose. If you rebuild `KPdfViewerConfig` inline on every recomposition, `rememberPdfViewerState(...)` will create a new viewer state and transient flows such as `openDocumentState` can reset.

If you want to create the state manually through the core library facade:

```kotlin
val sdk = KPdfFactory.create()
val viewerState = sdk.viewerState(
    source = KPdfSource.Url("https://example.com/file.pdf"),
    config = KPdfViewerConfig.builder().build(),
)
```

## 2. Configure The Viewer

`KPdfViewerConfig` controls runtime behavior for a viewer instance.

Available builder options:

- `enableZoom(Boolean)`
- `enableSwipe(Boolean)`
- `zoomRange(minZoom, maxZoom)`
- `doubleTapZoom(value)`
- `ramCacheSize(value)`
- `diskCacheSize(value)`
- `preloadPageCount(value)`

Example:

```kotlin
val config = KPdfViewerConfig.builder()
    .enableZoom(true)
    .enableSwipe(true)
    .zoomRange(minZoom = 1f, maxZoom = 4f)
    .doubleTapZoom(2.5f)
    .ramCacheSize(8)
    .diskCacheSize(50)
    .preloadPageCount(1)
    .build()
```

## 3. Control The Viewer

`KPdfViewerState` is the shared state holder for one PDF viewer instance.

### Observable State

- `source`
- `config`
- `loadState`
- `currentPageIndex`
- `currentZoom`
- `searchState`
- `renderedPage`
- `openDocumentState`
- `saveState`

### Main Actions

- `open(source)`
- `retry()`
- `nextPage()`
- `previousPage()`
- `goToPage(index)`
- `setZoom(zoom)`
- `zoomIn()`
- `zoomOut()`
- `resetZoom()`
- `searchText(query)`
- `nextSearchResult()`
- `previousSearchResult()`
- `clearSearch()`
- `requestOpenFromDevice()`
- `requestSave()`
- `savePdf()`
- `requestOpenInExternalApp()`
- `openInExternalApp()`
- `renderPage(...)`
- `exportPdf()`
- `close()`

### Basic Navigation Example

```kotlin
Button(onClick = { viewerState.previousPage() }) {
    Text("Previous")
}

Button(onClick = { viewerState.nextPage() }) {
    Text("Next")
}

Button(onClick = { viewerState.goToPage(10) }) {
    Text("Go To Page 11")
}
```

### Zoom Example

```kotlin
Button(onClick = { viewerState.zoomOut() }) {
    Text("Zoom Out")
}

Button(onClick = { viewerState.zoomIn() }) {
    Text("Zoom In")
}

Button(onClick = { viewerState.resetZoom() }) {
    Text("Reset Zoom")
}
```

### Search Example

Use `searchText(query)` to search the opened document. When matches are found, the viewer moves to the first match. Android 15+ and iOS also show highlighted match bounds on the rendered page.

```kotlin
val searchState by viewerState.searchState.collectAsState()

Button(onClick = { viewerState.searchText("total") }) {
    Text("Search")
}

Button(onClick = { viewerState.previousSearchResult() }) {
    Text("Previous Match")
}

Button(onClick = { viewerState.nextSearchResult() }) {
    Text("Next Match")
}

when (val state = searchState) {
    KPdfSearchState.Idle -> Unit
    is KPdfSearchState.Searching -> Text("Searching...")
    is KPdfSearchState.Success -> Text("${state.results.size} matches")
    is KPdfSearchState.Error -> Text(state.reason.message)
}
```

The connected toolbar can trigger search and navigate matches too. Pass `searchQuery` for the default Search chip behavior, or override `onSearchClick` when your app opens a custom search field or dialog.

```kotlin
KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
)
```

For a controlled search field:

```kotlin
var query by remember { mutableStateOf("") }

KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
    searchQuery = query,
    onSearchQueryChange = { query = it },
)
```

## 4. Render The Main Viewer

Use `KPdfViewer` to display the current page.

```kotlin
KPdfViewer(
    state = viewerState,
    modifier = Modifier.fillMaxSize(),
)
```

`KPdfViewer` reads the active page and zoom state from `KPdfViewerState`.

For a continuous vertical scroll layout, use `KPdfVerticalViewer` with the same state.

```kotlin
KPdfVerticalViewer(
    state = viewerState,
    modifier = Modifier.fillMaxSize(),
)
```

Both viewer layouts expose slots for loading and error states:

```kotlin
KPdfViewer(
    state = viewerState,
    loadingContent = { CircularProgressIndicator() },
    errorContent = { message -> Text(message) },
)

KPdfVerticalViewer(
    state = viewerState,
    loadingContent = { CircularProgressIndicator() },
    errorContent = { message -> Text(message) },
)
```

## 5. Add Connected Views

KPDF supports additional views that are independent in UI behavior but connected through the same `KPdfViewerState`.

### Thumbnail Strip

`KPdfThumbnailStrip` renders low-quality page thumbnails for better performance and uses the same viewer state as the main page surface.

```kotlin
KPdfThumbnailStrip(
    state = viewerState,
    onPageClick = { pageIndex ->
        viewerState.goToPage(pageIndex)
    }
)
```

Disable click handling completely:

```kotlin
KPdfThumbnailStrip(
    state = viewerState,
    onPageClick = null
)
```

Customize the strip:

```kotlin
KPdfThumbnailStrip(
    state = viewerState,
    thumbnailWidth = 92.dp,
    thumbnailHeight = 128.dp,
    style = KPdfThumbnailStripStyle.defaults().copy(
        stripContainerColor = Color(0xFFF4EFE6),
        selectedBorderColor = Color(0xFFB45F06),
        selectedPageNumberColor = Color(0xFFB45F06),
    )
)
```

### Toolbar View

`KPdfViewerToolbar` is a connected toolbar that can show page summary, search controls, zoom controls, save, share, and thumbnail toggle actions.

```kotlin
var thumbnailsVisible by remember { mutableStateOf(true) }

KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
    onShareClick = {
        // App-specific share implementation
    }
)
```

## 6. Customize The Toolbar

The toolbar is built around configuration objects so applications can replace strings, icons, visibility, and visual style from outside.

### Toolbar Config Objects

- `KPdfViewerToolbarConfig`
- `KPdfViewerToolbarVisibility`
- `KPdfViewerToolbarStrings`
- `KPdfViewerToolbarIcons`
- `KPdfViewerToolbarStyle`

### Toolbar Customization Example

```kotlin
KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
    onShareClick = { sharePdf() },
    config = KPdfViewerToolbarConfig.defaults().copy(
        visibility = KPdfViewerToolbarVisibility(
            showPageSummary = true,
            showSearch = true,
            showSearchSummary = true,
            showSearchResultNavigation = true,
            showClearSearch = true,
            showZoomOut = true,
            showZoomPercentage = true,
            showZoomIn = true,
            showSave = true,
            showShare = true,
            showThumbnailToggle = true,
        ),
        strings = KPdfViewerToolbarStrings.defaults().copy(
            searchText = "Find",
            searchPlaceholderText = "Search document",
            previousSearchResultText = "Previous",
            nextSearchResultText = "Next",
            zoomOutText = "Smaller",
            zoomInText = "Bigger",
            saveText = "Export",
            shareText = "Send",
            thumbnailToggleText = { visible ->
                if (visible) "Close Strip" else "Open Strip"
            },
        ),
        icons = KPdfViewerToolbarIcons.defaults().copy(
            searchIcon = { Text("F") },
            zoomOutIcon = { Text("--") },
            zoomInIcon = { Text("++") },
            saveIcon = { Text("SV") },
            shareIcon = { Text("SH") },
        ),
    ),
    style = KPdfViewerToolbarStyle.defaults().copy(
        minChipWidth = 96.dp,
    )
)
```

## 7. Open A PDF From Device

The save/open platform effects are already bound internally by `rememberPdfViewerState`, so applications usually only need to observe the state and react to success.

```kotlin
val openState by viewerState.openDocumentState.collectAsState()

LaunchedEffect(openState) {
    val selectedSource = (openState as? KPdfOpenDocumentState.Success)?.source
        ?: return@LaunchedEffect

    viewerState.open(selectedSource)
}

Button(onClick = { viewerState.requestOpenFromDevice() }) {
    Text("Open Local PDF")
}
```

Troubleshooting:
- If `openDocumentState` stays at `Idle`, make sure the `viewerState` is not being recreated by a newly built `KPdfViewerConfig` on every recomposition.
- If the picker returns `Success`, call `viewerState.open(selectedSource)` to replace the current document.

## 8. Save The Current PDF

```kotlin
val saveState by viewerState.saveState.collectAsState()

Button(onClick = { viewerState.requestSave() }) {
    Text("Save")
}

when (saveState) {
    KPdfSaveState.Idle -> Unit
    KPdfSaveState.Exporting -> Text("Preparing PDF...")
    is KPdfSaveState.AwaitingDestination -> Text("Choose where to save the file.")
    is KPdfSaveState.Success -> Text("PDF saved.")
    is KPdfSaveState.Cancelled -> Text("Save cancelled.")
    is KPdfSaveState.Error -> Text("Save failed.")
}
```

## 9. Open The Current PDF In An External App

Use `openInExternalApp()` when you want the library to hand the current PDF to any installed app that can open PDFs.

```kotlin
Button(onClick = { viewerState.openInExternalApp() }) {
    Text("Open In External App")
}
```

You can also pass a suggested file name:

```kotlin
viewerState.requestOpenInExternalApp(
    suggestedFileName = "invoice.pdf"
)
```

Observe the state if you want to react to success or failure:

```kotlin
val externalOpenState by viewerState.externalOpenState.collectAsState()
```

Typical usage:

```kotlin
when (externalOpenState) {
    KPdfExternalOpenState.Idle -> Unit
    KPdfExternalOpenState.Exporting -> Text("Preparing PDF...")
    is KPdfExternalOpenState.AwaitingExternalApp -> Text("Opening external app...")
    is KPdfExternalOpenState.Success -> Text("External app opened.")
    is KPdfExternalOpenState.Cancelled -> Text("Open was cancelled.")
    is KPdfExternalOpenState.Error -> Text("Unable to open external app.")
}
```

## 10. Share The Current PDF

KPDF does not force a platform sharing UI. Instead, the app can export the PDF bytes and pass them into its own Android/iOS share flow.

```kotlin
val scope = rememberCoroutineScope()

Button(
    onClick = {
        scope.launch {
            viewerState.exportPdf().fold(
                onSuccess = { pdfBytes ->
                    sharePdfBytes(pdfBytes)
                },
                onFailure = { error ->
                    println(error.message)
                }
            )
        }
    }
) {
    Text("Share")
}
```

## 11. Full Integration Example

```kotlin
@Composable
fun FullPdfScreen(source: KPdfSource) {
    val viewerState = rememberPdfViewerState(
        source = source,
        config = KPdfViewerConfig.builder()
            .preloadPageCount(1)
            .diskCacheSize(50)
            .build(),
    )
    var thumbnailsVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        KPdfViewerToolbar(
            state = viewerState,
            isThumbnailStripVisible = thumbnailsVisible,
            onThumbnailToggle = { thumbnailsVisible = it },
            onShareClick = {
                scope.launch {
                    viewerState.exportPdf().onSuccess { bytes ->
                        sharePdfBytes(bytes)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        KPdfViewer(
            state = viewerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        if (thumbnailsVisible) {
            KPdfThumbnailStrip(
                state = viewerState,
                onPageClick = { pageIndex ->
                    viewerState.goToPage(pageIndex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}
```

## Best Practices

- Use one `KPdfViewerState` per visible viewer instance.
- Reuse the same state across connected views like the main viewer, toolbar, and thumbnail strip.
- Use `KPdfThumbnailStrip` for page overview navigation instead of rendering full pages in a secondary surface.
- Use `openInExternalApp()` when you want the library to launch an installed PDF viewer app.
- Use `exportPdf()` for app-managed share flows.
- Use `requestSave()` when you want the library-integrated save/export path.
- Prefer `rememberPdfViewerState` in Compose unless you have a strong reason to manage lifecycle yourself.

## Notes

The examples in this document match the current public implementation in this repository.
