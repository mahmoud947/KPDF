package com.mahmoud.kpdf

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
            url =  "https://ontheline.trincoll.edu/images/bookdown/sample-local-pdf.pdf",
            headers = emptyMap(),
        )
        val kPdfState = rememberPdfViewerState(
            source = source,
        )

        KPdfViewer(
            state = kPdfState,
            modifier = Modifier.fillMaxSize()
        )
    }
}