package com.mahmoud.kpdf_core.source

import com.mahmoud.kpdf_core.api.KPdfSource


/*
 * Resolves public [KPdfSource] models into a normalized file/bytes pipeline.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
 interface KPdfSourceResolver {
     suspend fun resolve(source: KPdfSource): Result<ResolvedKPdfSource>
}
