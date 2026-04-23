package com.mahmoud.kpdf_core.repository

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage

actual class KPdfDocumentFactory {
    actual suspend fun open(
        filePath: String,
        documentId: String,
    ): Result<KPdfDocumentRef> = runCatching {
        val url = NSURL.fileURLWithPath(filePath)
        val document = PDFDocument(url)

        IosPdfDocumentRef(
            id = documentId,
            document = document,
        )
    }.recoverCatching {
        throw when (it) {
            is KPdfException -> it
            else -> KPdfException(KPdfError.Unknown(it.message ?: "Unable to open PDF document."), it)
        }
    }
}

private class IosPdfDocumentRef(
    override val id: String,
    private val document: PDFDocument,
) : KPdfDocumentRef {
    private val mutex = Mutex()
    private var closed = false

    override val pageCount: Int = document.pageCount.toInt()
    override val title: String? = document.documentAttributes?.get("Title") as? String

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> = mutex.withLock {
        runCatching {
            checkOpen()
            if (pageIndex !in 0 until pageCount) {
                throw KPdfException(KPdfError.Unknown("Page index out of bounds: $pageIndex"))
            }

            val page = document.pageAtIndex(pageIndex.toULong())
                ?: throw KPdfException(KPdfError.CorruptedDocument)
            val bounds = page.boundsForBox(kPDFDisplayBoxMediaBox)
            val pageSize = bounds.useContents {
                size.width to size.height
            }
            val target = calculateTargetSize(
                pageWidth = pageSize.first,
                pageHeight = pageSize.second,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                zoom = zoom,
            )

            UIGraphicsBeginImageContextWithOptions(target.toCGSize(), true, 0.0)
            val context = UIGraphicsGetCurrentContext()
                ?: throw KPdfException(KPdfError.Unknown("Unable to create image context."))

            UIColor.whiteColor.set()
            platform.CoreGraphics.CGContextFillRect(
                context,
                CGRectMake(0.0, 0.0, target.width, target.height),
            )
            platform.CoreGraphics.CGContextTranslateCTM(context, 0.0, target.height)
            platform.CoreGraphics.CGContextScaleCTM(context, target.width / pageSize.first, -target.height / pageSize.second)
            page.drawWithBox(kPDFDisplayBoxMediaBox, toContext = context)
            val image: UIImage = UIGraphicsGetImageFromCurrentImageContext()
                ?: throw KPdfException(KPdfError.Unknown("Unable to render PDF page image."))
            UIGraphicsEndImageContext()

            KPdfPageBitmap(
                image = KPlatformImage(image),
                width = image.size.useContents { width.toInt().coerceAtLeast(1) },
                height = image.size.useContents { height.toInt().coerceAtLeast(1) },
                pageIndex = pageIndex,
            )
        }.recoverCatching {
            throw when (it) {
                is KPdfException -> it
                else -> KPdfException(KPdfError.Unknown(it.message ?: "Unable to render PDF page."), it)
            }
        }
    }

    override fun close() {
        closed = true
    }

    private fun checkOpen() {
        if (closed) {
            throw KPdfException(KPdfError.Unknown("PDF document is already closed."))
        }
    }
}

private data class IosTargetSize(
    val width: Double,
    val height: Double,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun toCGSize(): kotlinx.cinterop.CValue<platform.CoreGraphics.CGSize> =
        platform.CoreGraphics.CGSizeMake(width, height)
}

private fun calculateTargetSize(
    pageWidth: Double,
    pageHeight: Double,
    targetWidth: Int,
    targetHeight: Int,
    zoom: Float,
): IosTargetSize {
    val safeWidth = targetWidth.coerceAtLeast(1).toDouble()
    val safeHeight = targetHeight.coerceAtLeast(1).toDouble()
    val pageAspect = pageWidth / pageHeight
    val boxAspect = safeWidth / safeHeight
    val base = if (pageAspect > boxAspect) {
        IosTargetSize(safeWidth, safeWidth / pageAspect)
    } else {
        IosTargetSize(safeHeight * pageAspect, safeHeight)
    }
    return IosTargetSize(
        width = (base.width * zoom).coerceAtLeast(1.0),
        height = (base.height * zoom).coerceAtLeast(1.0),
    )
}
