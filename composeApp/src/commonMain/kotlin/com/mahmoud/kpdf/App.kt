package com.mahmoud.kpdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_compose.rememberPdfViewerState
import com.mahmoud.kpdf_core.api.KPdfSaveState
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import kotlinx.coroutines.runBlocking
import kpdf.composeapp.generated.resources.Res

@Composable
@Preview
fun App() {
    MaterialTheme {
        val pdf = runBlocking { Res.readBytes("files/sample.pdf") }
        val source = KPdfSource.Bytes(pdf)
        val kPdfState = rememberPdfViewerState(
            source = source,
            config = KPdfViewerConfig.builder().preloadPageCount(1).diskCacheSize(50).build()
        )
        val saveState by kPdfState.saveState.collectAsState()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                KPdfViewer(
                    state = kPdfState,
                    modifier = Modifier.fillMaxSize().weight(4f)
                )
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
            }
        }
    }
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
