package com.mahmoud.kpdf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf.common.ScreenTabs
import com.mahmoud.kpdf.composeapp.generated.resources.Res
import com.mahmoud.kpdf.screens.Base64Screen
import com.mahmoud.kpdf.screens.ControlsScreen
import com.mahmoud.kpdf.screens.LocalScreen
import com.mahmoud.kpdf.screens.RemoteScreen
import com.mahmoud.kpdf.screens.ResourceScreen
import com.mahmoud.kpdf_compose.rememberPdfViewerState
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentState
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfSource.Base64
import com.mahmoud.kpdf_core.api.KPdfSource.Bytes
import kotlinx.coroutines.launch

/**
 * Created by Mahmoud kamal El-Din on 25/04/2026
 */

private const val RemoteDemoUrl = "https://exeterchessclub.org.uk/chessx/pdf/TacticsCourse.pdf"

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2563EB),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDBEAFE),
            onPrimaryContainer = Color(0xFF1E3A8A),
            secondary = Color(0xFF0F766E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDDF7F3),
            onSecondaryContainer = Color(0xFF134E4A),
            background = Color(0xFFF7F8FC),
            surface = Color.White,
            surfaceContainer = Color(0xFFF1F5F9),
            surfaceContainerLowest = Color(0xFFFBFCFE),
        ),
    ) {
        val sampleBytes by produceState<ByteArray?>(initialValue = null) {
            value = runCatching { Res.readBytes("files/sample.pdf") }.getOrNull()
        }
        val sampleBase64 by produceState<String?>(initialValue = null) {
            value = runCatching { Res.readBytes("files/base_64_pdf.text").decodeToString() }.getOrNull()
        }

        var currentScreen by remember { mutableStateOf(ShowcaseScreen.Remote) }
        var contentScreen by remember { mutableStateOf(ShowcaseScreen.Remote) }
        var importedSource by remember { mutableStateOf<KPdfSource?>(null) }
        var thumbnailsVisible by remember { mutableStateOf(true) }
        var status by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val config = remember { viewerConfig() }

        val remoteViewerState = rememberPdfViewerState(
            source = remember { KPdfSource.Url(RemoteDemoUrl) },
            config = config,
        )
        val base64ViewerState = rememberPdfViewerState(
            source = remember(sampleBase64, sampleBytes) {
                sampleBase64?.let(KPdfSource::Base64)
                    ?: sampleBytes?.let(KPdfSource::Bytes)
                    ?: KPdfSource.Url(RemoteDemoUrl)
            },
            config = config,
        )
        val resourceViewerState = rememberPdfViewerState(
            source = remember(sampleBytes, sampleBase64) {
                sampleBytes?.let(KPdfSource::Bytes)
                    ?: sampleBase64?.let(KPdfSource::Base64)
                    ?: KPdfSource.Url(RemoteDemoUrl)
            },
            config = config,
        )
        val localViewerState = rememberPdfViewerState(
            source = remember(importedSource, sampleBytes) {
                importedSource
                    ?: sampleBytes?.let(KPdfSource::Bytes)
                    ?: KPdfSource.Url(RemoteDemoUrl)
            },
            config = config,
        )

        val activeViewerState = when (contentScreen) {
            ShowcaseScreen.Remote -> remoteViewerState
            ShowcaseScreen.Base64 -> base64ViewerState
            ShowcaseScreen.Resource -> resourceViewerState
            ShowcaseScreen.Local -> localViewerState
            ShowcaseScreen.Controls -> remoteViewerState
        }
        val openDocumentState by localViewerState.openDocumentState.collectAsState()
        val externalOpenState by activeViewerState.externalOpenState.collectAsState()

        LaunchedEffect(openDocumentState) {
            val source = (openDocumentState as? KPdfOpenDocumentState.Success)?.source ?: return@LaunchedEffect
            importedSource = source
            contentScreen = ShowcaseScreen.Local
            currentScreen = ShowcaseScreen.Local
            status = "Imported"
        }

        LaunchedEffect(externalOpenState) {
            externalOpenState.toStatus()?.let { status = it }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widePadding = if (maxWidth >= 900.dp) 24.dp else 16.dp

            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceContainerLowest,
                                ),
                            ),
                        )
                        .padding(padding)
                        .padding(horizontal = widePadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ScreenTabs(
                        current = currentScreen,
                        onSelect = { screen ->
                            currentScreen = screen
                            if (screen != ShowcaseScreen.Controls) {
                                contentScreen = screen
                            }
                        },
                    )

                    when (currentScreen) {
                        ShowcaseScreen.Remote -> RemoteScreen(
                            state = remoteViewerState,
                            thumbnailsVisible = thumbnailsVisible,
                            onToggleThumbnails = { thumbnailsVisible = it },
                            onImportClick = { localViewerState.requestOpenFromDevice() },
                        )

                        ShowcaseScreen.Base64 -> Base64Screen(
                            state = base64ViewerState,
                            thumbnailsVisible = thumbnailsVisible,
                            onToggleThumbnails = { thumbnailsVisible = it },
                            onImportClick = { localViewerState.requestOpenFromDevice() },
                        )

                        ShowcaseScreen.Resource -> ResourceScreen(
                            state = resourceViewerState,
                            thumbnailsVisible = thumbnailsVisible,
                            onToggleThumbnails = { thumbnailsVisible = it },
                            onImportClick = { localViewerState.requestOpenFromDevice() },
                        )

                        ShowcaseScreen.Local -> LocalScreen(
                            state = localViewerState,
                            hasImportedSource = importedSource != null,
                            thumbnailsVisible = thumbnailsVisible,
                            onToggleThumbnails = { thumbnailsVisible = it },
                            onImportClick = { localViewerState.requestOpenFromDevice() },
                        )

                        ShowcaseScreen.Controls -> ControlsScreen(
                            state = activeViewerState,
                            thumbnailsVisible = thumbnailsVisible,
                            onToggleThumbnails = { thumbnailsVisible = !thumbnailsVisible },
                            onShare = {
                                scope.launch {
                                    status = activeViewerState.exportPdf().fold(
                                        onSuccess = { "Exported ${it.size} bytes" },
                                        onFailure = { error -> error.message ?: "Export failed" },
                                    )
                                }
                            },
                            onImportClick = { localViewerState.requestOpenFromDevice() },
                            onOpenExternal = { activeViewerState.requestOpenInExternalApp() },
                        )
                    }
                }
            }
        }
    }
}
