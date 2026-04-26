# KPDF Integration Guide

This guide shows the recommended integration path for KPDF in a Compose Multiplatform application.

## 1. Add The Library Modules

Add:

```kotlin
implementation("com.mahmoud.kpdf:kpdf-core:$version")
implementation("com.mahmoud.kpdf:kpdf-compose:$version")
```

Use:

- `kpdf-core` for shared PDF engine APIs
- `kpdf-compose` for the Compose viewer and platform save/open integrations

## 2. Create A Source

```kotlin
val source = KPdfSource.Url("https://example.com/file.pdf")
```

Other supported options:

- `KPdfSource.Bytes(...)`
- `KPdfSource.Base64(...)`

## 3. Create Viewer State

```kotlin
val stableSource = remember(source) { source }
val viewerConfig = remember {
    KPdfViewerConfig.builder()
        .enableSwipe(true)
        .preloadPageCount(1)
        .diskCacheSize(50)
        .build(),
}

val viewerState = rememberPdfViewerState(
    source = stableSource,
    config = viewerConfig,
)
```

Important: keep `source` and `config` stable with `remember(...)`. Rebuilding `KPdfViewerConfig` inline on each recomposition will recreate the viewer state and reset transient flows such as `openDocumentState`.

## 4. Render The Viewer

```kotlin
KPdfViewer(
    state = viewerState,
    modifier = Modifier.fillMaxSize(),
)
```

## 5. Add Optional Connected Views

### Toolbar

```kotlin
var thumbnailsVisible by remember { mutableStateOf(true) }

KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
    onShareClick = { /* custom share */ },
)
```

### Thumbnail Strip

```kotlin
if (thumbnailsVisible) {
    KPdfThumbnailStrip(
        state = viewerState,
        onPageClick = { pageIndex ->
            viewerState.goToPage(pageIndex)
        },
    )
}
```

## 6. Handle Open From Device

```kotlin
val openState by viewerState.openDocumentState.collectAsState()

LaunchedEffect(openState) {
    val selectedSource = (openState as? KPdfOpenDocumentState.Success)?.source
        ?: return@LaunchedEffect

    viewerState.open(selectedSource)
}
```

Trigger the picker:

```kotlin
viewerState.requestOpenFromDevice()
```

If the picker flow appears stuck in `Idle`, verify that your composable is not recreating the `viewerState` by passing a brand-new `KPdfViewerConfig` instance on every recomposition.

## 7. Handle Save

```kotlin
viewerState.requestSave()
```

Observe save progress:

```kotlin
val saveState by viewerState.saveState.collectAsState()
```

## 8. Open In External PDF Viewer

```kotlin
viewerState.openInExternalApp()
```

With a custom file name:

```kotlin
viewerState.requestOpenInExternalApp(
    suggestedFileName = "report.pdf"
)
```

Observe the state:

```kotlin
val externalOpenState by viewerState.externalOpenState.collectAsState()
```

## 9. Handle Share

Use `exportPdf()` and route the bytes into your own platform share flow:

```kotlin
val scope = rememberCoroutineScope()

scope.launch {
    viewerState.exportPdf().onSuccess { bytes ->
        sharePdfBytes(bytes)
    }
}
```

## 10. Read More

For the full usage guide and advanced customization examples, see [Library guide](SDK.md).
