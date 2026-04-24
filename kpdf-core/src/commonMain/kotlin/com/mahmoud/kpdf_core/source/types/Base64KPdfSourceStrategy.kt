package com.mahmoud.kpdf_core.source.types

import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.error.kpdfFailure
import com.mahmoud.kpdf_core.source.KPdfSourceStrategy
import com.mahmoud.kpdf_core.source.ResolvedKPdfSource
import com.mahmoud.kpdf_core.utils.normalizedBase64
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-24.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

internal class Base64KPdfSourceStrategy : KPdfSourceStrategy<KPdfSource.Base64> {
    override fun canResolve(source: KPdfSource): Boolean = source is KPdfSource.Base64

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun resolve(source: KPdfSource.Base64): Result<ResolvedKPdfSource> {
        val normalizedValue = source.value.normalizedBase64()
        if (normalizedValue.isBlank()) {
            return kpdfFailure(
                KPdfError.Unknown("Base64 PDF source is empty.")
            )
        }

        val decodedBytes = runCatching {
            Base64.Default.decode(normalizedValue)
        }.getOrElse { throwable ->
            return kpdfFailure(
                KPdfException(
                    KPdfError.Unknown("Invalid Base64 PDF source."),
                    throwable,
                )
            )
        }

        if (decodedBytes.isEmpty()) {
            return kpdfFailure(
                KPdfError.Unknown("Decoded Base64 PDF source is empty.")
            )
        }

        return Result.success(ResolvedKPdfSource.Bytes(decodedBytes))
    }
}
