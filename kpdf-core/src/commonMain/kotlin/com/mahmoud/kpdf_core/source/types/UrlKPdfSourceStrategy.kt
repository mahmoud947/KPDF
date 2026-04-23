package com.mahmoud.kpdf_core.source.types

import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.error.DefaultKPdfErrorMapper
import com.mahmoud.kpdf_core.error.KPdfErrorMapper
import com.mahmoud.kpdf_core.error.KPdfIoException
import com.mahmoud.kpdf_core.error.kpdfFailure
import com.mahmoud.kpdf_core.filesystem.KPdfFileSystem
import com.mahmoud.kpdf_core.network.KPdfRemoteDataSource
import com.mahmoud.kpdf_core.source.KPdfSourceStrategy
import com.mahmoud.kpdf_core.source.ResolvedKPdfSource
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Resolves remote PDFs by streaming them to a temporary file.
 */
 class UrlKPdfSourceStrategy(
    private val remoteDataSource: KPdfRemoteDataSource,
    private val fileSystem: KPdfFileSystem,
    private val errorMapper: KPdfErrorMapper = DefaultKPdfErrorMapper,
) : KPdfSourceStrategy<KPdfSource.Url> {
    override fun canResolve(source: KPdfSource): Boolean = source is KPdfSource.Url

    override suspend fun resolve(source: KPdfSource.Url): Result<ResolvedKPdfSource> {
        val tempPath = runCatching {
            fileSystem.createTempPdfFile(prefix = "pdf-url")
        }.getOrElse { return kpdfFailure(KPdfIoException("Unable to create temp PDF file.", it), errorMapper) }

        coroutineContext.ensureActive()

        val downloadResult = remoteDataSource.downloadToFile(
            url = source.url,
            headers = source.headers,
            destinationPath = tempPath,
        )

        return downloadResult.fold(
            onSuccess = {
                coroutineContext.ensureActive()
                Result.success(ResolvedKPdfSource.File(path = it.filePath, temporary = true))
            },
            onFailure = { throwable ->
                fileSystem.delete(tempPath)
                kpdfFailure(throwable, errorMapper)
            },
        )
    }
}
