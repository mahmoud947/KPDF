package com.mahmoud.kpdf

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.mahmoud.kpdf_core.api.KPdfSource

@Composable
@Preview
fun App() {
    MaterialTheme {
       val source = KPdfSource.Url(
            url =  "https://ontheline.trincoll.edu/images/bookdown/sample-local-pdf.pdf",
            headers = emptyMap(),
        )
    }
}