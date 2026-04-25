# KPDF SDK Documentation

KPDF is a Kotlin Multiplatform PDF SDK for Android and iOS with a Compose Multiplatform viewer layer.

This document focuses on the public SDK surface and how to use it in real applications.

## Modules

- `kpdf-core`
  Shared PDF source loading, rendering, caching, navigation, zoom, save/export, and picker state APIs.
- `kpdf-compose`
  Compose Multiplatform UI components and platform integration for save/open flows.
- `composeApp`
  Sample application that demonstrates how the SDK is intended to be used.

## Main Concepts

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

## Creating Viewer State

In Compose, the easiest entry point is `rememberPdfViewerState`.

```kotlin
@Composable
fun PdfScreen(source: KPdfSource) {
    val viewerState = rememberPdfViewerState(
        source = source,
        config = KPdfViewerConfig.builder()
            .zoomRange(minZoom = 1f, maxZoom = 5f)
            .doubleTapZoom(2f)
            .ramCacheSize(6)
            .diskCacheSize(24)
            .preloadPageCount(2)
            .build(),
    )

    KPdfViewer(state = viewerState)
}
```

If you want to create the state manually through the core SDK facade:

```kotlin
val sdk = KPdfFactory.create()
val viewerState = sdk.viewerState(
    source = KPdfSource.Url("https://example.com/file.pdf"),
    config = KPdfViewerConfig.builder().build(),
)
```

## KPdfViewerConfig

`KPdfViewerConfig` controls runtime behavior for a viewer instance.

Available builder options:

- `enableZoom(Boolean)`
- `zoomRange(minZoom, maxZoom)`
- `doubleTapZoom(value)`
- `ramCacheSize(value)`
- `diskCacheSize(value)`
- `preloadPageCount(value)`

Example:

```kotlin
val config = KPdfViewerConfig.builder()
    .enableZoom(true)
    .zoomRange(minZoom = 1f, maxZoom = 4f)
    .doubleTapZoom(2.5f)
    .ramCacheSize(8)
    .diskCacheSize(50)
    .preloadPageCount(1)
    .build()
```

## KPdfViewerState

`KPdfViewerState` is the shared state holder for one PDF viewer instance.

### Observable State

- `source`
- `config`
- `loadState`
- `currentPageIndex`
- `currentZoom`
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
- `requestOpenFromDevice()`
- `requestSave()`
- `savePdf()`
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

## Main Viewer

Use `KPdfViewer` to display the current page.

```kotlin
KPdfViewer(
    state = viewerState,
    modifier = Modifier.fillMaxSize(),
)
```

`KPdfViewer` reads the active page and zoom state from `KPdfViewerState`.

## Connected Secondary Views

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

`KPdfViewerToolbar` is a connected toolbar that can show page summary, zoom controls, save, share, and thumbnail toggle actions.

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

## Fully Configurable Toolbar

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
            showZoomOut = true,
            showZoomPercentage = true,
            showZoomIn = true,
            showSave = true,
            showShare = true,
            showThumbnailToggle = true,
        ),
        strings = KPdfViewerToolbarStrings.defaults().copy(
            zoomOutText = "Smaller",
            zoomInText = "Bigger",
            saveText = "Export",
            shareText = "Send",
            thumbnailToggleText = { visible ->
                if (visible) "Close Strip" else "Open Strip"
            },
        ),
        icons = KPdfViewerToolbarIcons.defaults().copy(
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

## Open From Device Flow

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

## Save Flow

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

## Share Flow

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

## Full Screen Example

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

## Notes And Best Practices

- Use one `KPdfViewerState` per visible viewer instance.
- Reuse the same state across connected views like the main viewer, toolbar, and thumbnail strip.
- Use `KPdfThumbnailStrip` for page overview navigation instead of rendering full pages in a secondary surface.
- Use `exportPdf()` for app-managed share flows.
- Use `requestSave()` when you want the SDK-integrated save/export path.
- Prefer `rememberPdfViewerState` in Compose unless you have a strong reason to manage lifecycle yourself.

## Verification

The SDK documentation in this repository matches the current public implementation verified with:

```bash
./gradlew \
  :kpdf-core:compileCommonMainKotlinMetadata \
  :kpdf-core:compileAndroidMain \
  :kpdf-compose:compileCommonMainKotlinMetadata \
  :kpdf-compose:compileAndroidMain \
  :kpdf-compose:compileKotlinIosSimulatorArm64 \
  :composeApp:compileDebugSources
```
