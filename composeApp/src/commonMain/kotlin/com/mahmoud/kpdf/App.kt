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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mahmoud.kpdf_compose.KPdfViewer
import com.mahmoud.kpdf_compose.rememberPdfViewerState
import com.mahmoud.kpdf_core.api.KPdfSource

@Composable
@Preview
fun App() {
    MaterialTheme {
        val source = KPdfSource.Url(
            url = "https://www.princexml.com/samples/newsletter/drylab.pdf",
            headers = emptyMap(),
        )
        val kPdfState = rememberPdfViewerState(
            source = source,
        )
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
                }
            }
        }


    }
}