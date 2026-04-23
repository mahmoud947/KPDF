package com.mahmoud.kpdf_core.repository

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfSource

/**
 * Coordinates source resolution, document caching, opening, and lifecycle.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
interface KPdfRepository {
    suspend fun open(source: KPdfSource): Result<KPdfDocumentRef>

    suspend fun close(documentId: String)

    suspend fun closeAll()
}
