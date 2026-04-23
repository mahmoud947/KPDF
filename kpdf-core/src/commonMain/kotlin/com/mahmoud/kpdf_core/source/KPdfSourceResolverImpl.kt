package com.mahmoud.kpdf_core.source

import com.mahmoud.kpdf_core.api.KPdfSource

/*
 * Selects the source strategy for each public source type.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */



class KPdfSourceResolverImpl(
    private val urlStrategy: KPdfSourceStrategy<KPdfSource.Url>,
) : KPdfSourceResolver {
    override suspend fun resolve(source: KPdfSource): Result<ResolvedKPdfSource> =
        when (source) {
            is KPdfSource.Url -> urlStrategy.resolve(source)
        }
}
