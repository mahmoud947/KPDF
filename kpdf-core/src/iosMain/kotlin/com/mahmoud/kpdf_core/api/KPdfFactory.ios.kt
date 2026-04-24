package com.mahmoud.kpdf_core.api

import com.mahmoud.kpdf_core.DefaultKPdf
import com.mahmoud.kpdf_core.cache.IosKPdfPageBitmapCodec
import com.mahmoud.kpdf_core.cache.KPdfDiskPageCache
import com.mahmoud.kpdf_core.cache.KPdfFileSystemDiskCacheStorage
import com.mahmoud.kpdf_core.cache.KPdfLayeredPageCache
import com.mahmoud.kpdf_core.cache.KPdfMemoryPageCache
import com.mahmoud.kpdf_core.filesystem.KPdfFileSystem
import com.mahmoud.kpdf_core.network.KPdfRemoteDataSource
import com.mahmoud.kpdf_core.repository.DefaultKKPdfRepository
import com.mahmoud.kpdf_core.repository.KPdfDocumentFactory
import com.mahmoud.kpdf_core.source.KPdfSourceResolverImpl
import com.mahmoud.kpdf_core.source.types.UrlKPdfSourceStrategy

actual object KPdfFactory {
    actual fun create(): KPdf {
        val fileSystem = KPdfFileSystem()
        val remoteDataSource = KPdfRemoteDataSource()
        val resolver = KPdfSourceResolverImpl(
            urlStrategy = UrlKPdfSourceStrategy(
                remoteDataSource = remoteDataSource,
                fileSystem = fileSystem,
            ),
        )
        return DefaultKPdf(
            repository = DefaultKKPdfRepository(
                sourceResolver = resolver,
                fileSystem = fileSystem,
                documentFactory = KPdfDocumentFactory(),
            ),
            pageCacheFactory = { config ->
                KPdfLayeredPageCache(
                    memoryCache = KPdfMemoryPageCache(config.ramCacheSize),
                    diskCache = KPdfDiskPageCache(
                        storage = KPdfFileSystemDiskCacheStorage(fileSystem),
                        bitmapCodec = IosKPdfPageBitmapCodec(),
                        maxEntries = config.diskCacheSize,
                    ),
                )
            },
        )
    }
}
