package com.mahmoud.kpdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf_compose.KPdfThumbnailStrip
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_compose.KPdfViewerToolbar
import com.mahmoud.kpdf_compose.rememberPdfViewerState
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentState
import com.mahmoud.kpdf_core.api.KPdfSaveState
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        val source = KPdfSource.Url("https://exeterchessclub.org.uk/chessx/pdf/TacticsCourse.pdf")
        val kPdfState = rememberPdfViewerState(
            source = source,
            config = KPdfViewerConfig.builder().preloadPageCount(1).diskCacheSize(50).enableSwipe(true).build()
        )
        val scope = rememberCoroutineScope()
        var isThumbnailStripVisible by remember { mutableStateOf(true) }
        var shareMessage by remember { mutableStateOf<String?>(null) }
        val openDocumentState by kPdfState.openDocumentState.collectAsState()
        val saveState by kPdfState.saveState.collectAsState()

        LaunchedEffect(openDocumentState) {
            val selectedSource = (openDocumentState as? KPdfOpenDocumentState.Success)?.source
                ?: return@LaunchedEffect

            kPdfState.open(selectedSource)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                KPdfViewerToolbar(
                    state = kPdfState,
                    isThumbnailStripVisible = isThumbnailStripVisible,
                    onThumbnailToggle = { isThumbnailStripVisible = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    onShareClick = {
                        scope.launch {
                            shareMessage = kPdfState.exportPdf().fold(
                                onSuccess = { bytes -> "Share payload ready (${bytes.size} bytes)." },
                                onFailure = { throwable -> throwable.message ?: "Unable to prepare PDF share payload." },
                            )
                        }
                    },
                )
                KPdfViewer(
                    state = kPdfState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(4f)
                )
                if (isThumbnailStripVisible) {
                    KPdfThumbnailStrip(
                        state = kPdfState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .padding(horizontal = 12.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { kPdfState.previousPage() }) {
                        Text("Previous")
                    }
                    Button(onClick = { kPdfState.nextPage() }) {
                        Text("Next")
                    }
                    Button(
                        onClick = {
                            kPdfState.requestOpenFromDevice()
                        }
                    ) {
                        Text("Open Local")
                    }

                    Button(
                        onClick = {
                            kPdfState.requestOpenInExternalApp()
                        }
                    ) {
                        Text("Open On External app")
                    }
                    Button(
                        onClick = {
                            kPdfState.requestSave()
                        }
                    ) {
                        Text("Save")
                    }
                }
                saveMessage(saveState)?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                openDocumentMessage(openDocumentState)?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                shareMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

private fun openDocumentMessage(state: KPdfOpenDocumentState): String? =
    when (state) {
        is KPdfOpenDocumentState.Idle -> null
        is KPdfOpenDocumentState.AwaitingSelection -> "Choose a PDF to open."
        is KPdfOpenDocumentState.Success -> "PDF selected."
        is KPdfOpenDocumentState.Cancelled -> "Open PDF was cancelled."
        is KPdfOpenDocumentState.Error -> state.reason.message
    }

private fun saveMessage(saveState: KPdfSaveState): String? =
    when (saveState) {
        KPdfSaveState.Idle -> null
        KPdfSaveState.Exporting -> "Preparing the PDF for save..."
        is KPdfSaveState.AwaitingDestination -> "Choose where to save the PDF."
        is KPdfSaveState.Success -> "PDF saved successfully."
        is KPdfSaveState.Cancelled -> "Save was cancelled."
        is KPdfSaveState.Error -> saveState.reason.message
    }
