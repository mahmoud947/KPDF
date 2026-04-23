package com.mahmoud.kpdf_core

import com.mahmoud.kpdf_core.api.KPdf
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.KPdfViewerConfig
import com.mahmoud.kpdf_core.api.KPdfViewerState
import com.mahmoud.kpdf_core.viewer.DefaultKPdfViewerState

class DefaultKPdf: KPdf {
    override fun viewerState(
        source: KPdfSource,
        config: KPdfViewerConfig,
    ): KPdfViewerState =
        DefaultKPdfViewerState(
            source = source,
            config = config,
        )
}
