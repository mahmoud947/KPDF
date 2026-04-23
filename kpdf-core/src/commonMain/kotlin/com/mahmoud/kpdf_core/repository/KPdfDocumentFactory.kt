package com.mahmoud.kpdf_core.repository

import com.mahmoud.kpdf_core.api.KPdfDocumentRef

/**
 * Opens a platform PDF document from a file path.
 *
 * Android and iOS renderer adapters will implement this contract using
 * PdfRenderer and PDFKit respectively.
 */
 expect class KPdfDocumentFactory {
     suspend fun open(
        filePath: String,
        documentId: String,
    ): Result<KPdfDocumentRef>
}
