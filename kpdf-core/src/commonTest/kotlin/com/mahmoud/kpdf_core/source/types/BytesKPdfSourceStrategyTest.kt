package com.mahmoud.kpdf_core.source.types

import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.source.ResolvedKPdfSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BytesKPdfSourceStrategyTest {

    @Test
    fun resolve_returnsRawPdfBytes() {
        runBlocking {
            val source = KPdfSource.Bytes("%PDF-1.4\n".encodeToByteArray())

            val result = BytesKPdfSourceStrategy().resolve(source)

            assertTrue(result.isSuccess)
            val resolved = result.getOrThrow()
            assertIs<ResolvedKPdfSource.Bytes>(resolved)
            assertContentEquals("%PDF-1.4\n".encodeToByteArray(), resolved.data)
        }
    }

    @Test
    fun resolve_returnsFailure_forEmptyBytes() {
        runBlocking {
            val result = BytesKPdfSourceStrategy().resolve(KPdfSource.Bytes(byteArrayOf()))

            assertTrue(result.isFailure)
        }
    }
}
