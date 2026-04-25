package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import com.mahmoud.kpdf_core.api.KPdfExternalOpenRequest
import com.mahmoud.kpdf_core.api.KPdfExternalOpenResult

internal interface PdfExternalOpenLauncher {
    fun launch(
        request: KPdfExternalOpenRequest,
        onResult: (KPdfExternalOpenResult) -> Unit,
    )
}

@Composable
internal expect fun rememberPdfExternalOpenLauncher(): PdfExternalOpenLauncher
