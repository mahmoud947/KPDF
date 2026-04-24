package com.mahmoud.kpdf_core.source.types

import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.error.kpdfFailure
import com.mahmoud.kpdf_core.source.KPdfSourceStrategy
import com.mahmoud.kpdf_core.source.ResolvedKPdfSource

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class BytesKPdfSourceStrategy : KPdfSourceStrategy<KPdfSource.Bytes> {
    override fun canResolve(source: KPdfSource): Boolean = source is KPdfSource.Bytes

    override suspend fun resolve(source: KPdfSource.Bytes): Result<ResolvedKPdfSource> {
        if (source.data.isEmpty()) {
            return kpdfFailure(
                KPdfError.Unknown("PDF bytes source is empty.")
            )
        }

        return Result.success(ResolvedKPdfSource.Bytes(source.data))
    }
}
