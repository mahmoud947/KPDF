package com.mahmoud.kpdf_core.repository

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfSource
import com.mahmoud.kpdf_core.error.DefaultKPdfErrorMapper
import com.mahmoud.kpdf_core.error.KPdfErrorMapper
import com.mahmoud.kpdf_core.error.KPdfIoException
import com.mahmoud.kpdf_core.error.kpdfFailure
import com.mahmoud.kpdf_core.filesystem.KPdfFileSystem
import com.mahmoud.kpdf_core.source.KPdfSourceResolver
import com.mahmoud.kpdf_core.source.ResolvedKPdfSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/*
 * Default repository implementation for the common source pipeline.
 *
 * Created by Mahmoud Kamal El-Din on 2026-04-23.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
class DefaultKKPdfRepository(
    private val sourceResolver: KPdfSourceResolver,
    private val fileSystem: KPdfFileSystem,
    private val documentFactory: KPdfDocumentFactory,
    private val errorMapper: KPdfErrorMapper = DefaultKPdfErrorMapper,
) : KPdfRepository {

    private val mutex = Mutex()
    private val openDocuments = mutableMapOf<String, OpenDocumentRecord>()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun open(source: KPdfSource): Result<KPdfDocumentRef> {
        val materialized = materialize(source).getOrElse {
            return kpdfFailure(it, errorMapper)
        }

        val document = documentFactory.open(
            documentId = Uuid.random().toString(),
            filePath = materialized.path,
        ).getOrElse { throwable ->
            cleanup(materialized)
            return kpdfFailure(throwable, errorMapper)
        }

        mutex.withLock {
            openDocuments.remove(document.id)?.closeAndCleanup()
            openDocuments[document.id] = OpenDocumentRecord(
                document = document,
                temporaryFiles = listOfNotNull(
                    materialized.path.takeIf { materialized.temporary }
                ),
            )
        }

        return Result.success(document)
    }

    override suspend fun export(source: KPdfSource): Result<ByteArray> {
        val materialized = materialize(source).getOrElse {
            return kpdfFailure(it, errorMapper)
        }

        return runCatching {
            val bytes = fileSystem.readBytes(materialized.path)
                ?: throw KPdfIoException("Unable to read PDF bytes from source.")
            bytes
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable -> kpdfFailure(throwable, errorMapper) }
        ).also {
            cleanup(materialized)
        }
    }

    override suspend fun close(documentId: String) {
        val record = mutex.withLock {
            openDocuments.remove(documentId)
        } ?: return

        record.closeAndCleanup()
    }

    override suspend fun closeAll() {
        val records = mutex.withLock {
            openDocuments.values.toList().also {
                openDocuments.clear()
            }
        }

        records.forEach { it.closeAndCleanup() }
    }

    private suspend fun materialize(
        source: KPdfSource,
    ): Result<MaterializedPdfFile> {
        val resolved = sourceResolver.resolve(source)
            .getOrElse { return kpdfFailure(it, errorMapper) }

        val materialized = when (resolved) {
            is ResolvedKPdfSource.File -> {
                MaterializedPdfFile(
                    path = resolved.path,
                    temporary = resolved.temporary,
                )
            }

            is ResolvedKPdfSource.Bytes -> {
                val tempPath = runCatching {
                    fileSystem.createTempPdfFile(prefix = "pdf-bytes")
                }.getOrElse {
                    return kpdfFailure(
                        KPdfIoException("Unable to create temp PDF file.", it),
                        errorMapper
                    )
                }

                runCatching {
                    fileSystem.writeBytes(tempPath, resolved.data)
                }.getOrElse {
                    fileSystem.delete(tempPath)
                    return kpdfFailure(
                        KPdfIoException("Unable to write PDF bytes to temp file.", it),
                        errorMapper
                    )
                }

                MaterializedPdfFile(
                    path = tempPath,
                    temporary = true,
                )
            }
        }

        return Result.success(materialized)
    }

    private suspend fun cleanup(file: MaterializedPdfFile) {
        if (file.temporary) {
            fileSystem.delete(file.path)
        }
    }

    private suspend fun OpenDocumentRecord.closeAndCleanup() {
        document.close()
        temporaryFiles.forEach { fileSystem.delete(it)  }
    }
}

private data class MaterializedPdfFile(
    val path: String,
    val temporary: Boolean,
)

private data class OpenDocumentRecord(
    val document: KPdfDocumentRef,
    val temporaryFiles: List<String>,
)
