package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import com.mahmoud.kpdf_core.api.KPdfSaveRequest
import com.mahmoud.kpdf_core.api.KPdfSaveResult

internal interface PdfSaveLauncher {
    fun launch(
        request: KPdfSaveRequest,
        onResult: (KPdfSaveResult) -> Unit,
    )
}

@Composable
internal expect fun rememberPdfSaveLauncher(): PdfSaveLauncher
