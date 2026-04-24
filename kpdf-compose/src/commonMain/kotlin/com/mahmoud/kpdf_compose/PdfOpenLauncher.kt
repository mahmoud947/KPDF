package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentRequest
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentResult

internal interface PdfOpenLauncher {
    fun launch(
        request: KPdfOpenDocumentRequest,
        onResult: (KPdfOpenDocumentResult) -> Unit,
    )
}

@Composable
internal expect fun rememberPdfOpenLauncher(): PdfOpenLauncher
