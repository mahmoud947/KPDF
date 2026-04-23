package com.mahmoud.kpdf_core.source

import com.mahmoud.kpdf_core.api.KPdfSource

/*
 * Strategy for resolving a concrete [KPdfSource] subtype.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
interface KPdfSourceStrategy<T : KPdfSource> {
    fun canResolve(source: KPdfSource): Boolean

    suspend fun resolve(source: T): Result<ResolvedKPdfSource>
}
