package com.mahmoud.kpdf_core.source.types

import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.api.cacheKey
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
        val cachedFilePath = runCatching {
            buildCachedPdfPath(source)
        }.getOrElse {
            return kpdfFailure(KPdfIoException("Unable to resolve PDF cache path.", it), errorMapper)
        }

        val tempPath = runCatching {
            fileSystem.createTempPdfFile(prefix = "pdf-url")
        }.getOrElse {
            if (fileSystem.isReadable(cachedFilePath)) {
                return Result.success(
                    ResolvedKPdfSource.File(
                        path = cachedFilePath,
                        temporary = false,
                    )
                )
            }

            return kpdfFailure(KPdfIoException("Unable to create temp PDF file.", it), errorMapper)
        }

        coroutineContext.ensureActive()

        val downloadResult = remoteDataSource.downloadToFile(
            url = source.url,
            headers = source.headers,
            destinationPath = tempPath,
        )

        return downloadResult.fold(
            onSuccess = {
                coroutineContext.ensureActive()

                runCatching {
                    fileSystem.delete(cachedFilePath)
                    fileSystem.copyFile(tempPath, cachedFilePath)
                    fileSystem.delete(tempPath)
                }.fold(
                    onSuccess = {
                        Result.success(
                            ResolvedKPdfSource.File(
                                path = cachedFilePath,
                                temporary = false,
                            )
                        )
                    },
                    onFailure = { throwable ->
                        fileSystem.delete(tempPath)
                        kpdfFailure(
                            KPdfIoException("Unable to persist downloaded PDF.", throwable),
                            errorMapper,
                        )
                    },
                )
            },
            onFailure = { throwable ->
                fileSystem.delete(tempPath)

                if (fileSystem.isReadable(cachedFilePath)) {
                    Result.success(
                        ResolvedKPdfSource.File(
                            path = cachedFilePath,
                            temporary = false,
                        )
                    )
                } else {
                    kpdfFailure(throwable, errorMapper)
                }
            },
        )
    }

    private suspend fun buildCachedPdfPath(source: KPdfSource.Url): String {
        val cacheDirectory = fileSystem.createCacheDirectory(name = SourceCacheDirectoryName)
        return cacheDirectory.trimEnd('/') + "/" + source.cacheKey().fingerprint() + ".pdf"
    }

    private fun String.fingerprint(): String {
        var hash = 1469598103934665603UL
        encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toULong()
            hash *= 1099511628211UL
        }
        return hash.toString(16)
    }

    private companion object {
        const val SourceCacheDirectoryName = "kpdf-source-cache"
    }
}
