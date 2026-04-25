# KPDF

KPDF is a Kotlin Multiplatform PDF library for Android and iOS with a Compose Multiplatform viewer layer.

## Modules

- `kpdf-core`: shared PDF loading, rendering, caching, save/export, and local picker state APIs
- `kpdf-compose`: Compose viewer and platform integrations for save/open flows
- `composeApp`: sample app used to exercise the library

## Current Features

- URL, Base64, bytes, and resource-backed PDF sources
- RAM page cache
- disk page cache
- remote source persistence for offline reopen
- configurable page preloading
- shared zoom state
- save/export flow from `KPdfViewerState`
- open-from-device flow from `KPdfViewerState`
- connected toolbar view
- connected thumbnail strip view
- Android and iOS Compose support

## Quick Start

```kotlin
@Composable
fun PdfScreen(source: KPdfSource) {
    val viewerState = rememberPdfViewerState(
        source = source,
        config = KPdfViewerConfig.builder()
            .ramCacheSize(6)
            .diskCacheSize(24)
            .preloadPageCount(2)
            .build(),
    )

    KPdfViewer(state = viewerState)
}
```

## Connected Views

KPDF also exposes optional connected views that share the same `KPdfViewerState`.

```kotlin
var thumbnailsVisible by remember { mutableStateOf(true) }

KPdfViewerToolbar(
    state = viewerState,
    isThumbnailStripVisible = thumbnailsVisible,
    onThumbnailToggle = { thumbnailsVisible = it },
    onShareClick = { /* custom share flow */ },
)

KPdfViewer(state = viewerState)

if (thumbnailsVisible) {
    KPdfThumbnailStrip(
        state = viewerState,
        onPageClick = { pageIndex ->
            viewerState.goToPage(pageIndex)
        },
    )
}
```

## Open A Local PDF

The library returns the selected source through `openDocumentState`. The app decides whether to replace the current document.

```kotlin
val openState by viewerState.openDocumentState.collectAsState()

LaunchedEffect(openState) {
    val selectedSource = (openState as? KPdfOpenDocumentState.Success)?.source
        ?: return@LaunchedEffect

    viewerState.open(selectedSource)
}

Button(onClick = { viewerState.requestOpenFromDevice() }) {
    Text("Open Local")
}
```

## Save The Current PDF

```kotlin
Button(onClick = { viewerState.requestSave() }) {
    Text("Save")
}
```

## Documentation

- SDK guide: [docs/SDK.md](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/docs/SDK.md:1)
- Integration guide: [docs/INTEGRATION.md](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/docs/INTEGRATION.md:1)
- Deployment guide: [docs/DEPLOYMENT.md](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/docs/DEPLOYMENT.md:1)

## Prepare For Publishing

Publishing infrastructure is included for `kpdf-core` and `kpdf-compose`.

Before publishing externally, replace the placeholder metadata in [gradle.properties](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/gradle.properties:1), then follow [docs/DEPLOYMENT.md](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/docs/DEPLOYMENT.md:1).

## Verification

```bash
./gradlew \
  :kpdf-core:testAndroidHostTest \
  :kpdf-core:compileKotlinIosSimulatorArm64 \
  :kpdf-compose:compileKotlinIosSimulatorArm64 \
  :composeApp:compileDebugKotlinAndroid
```
