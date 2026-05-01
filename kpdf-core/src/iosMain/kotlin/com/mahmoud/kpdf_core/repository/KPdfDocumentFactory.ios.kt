package com.mahmoud.kpdf_core.repository

import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.api.KPdfSearchRect
import com.mahmoud.kpdf_core.api.KPdfSearchResult
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFSelection
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

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun searchText(query: String): Result<List<KPdfSearchResult>> = mutex.withLock {
        runCatching {
            checkOpen()
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return@runCatching emptyList()
            }

            val selections = document.findString(
                string = normalizedQuery,
                withOptions = NSCaseInsensitiveSearch,
            )

            buildList {
                selections.forEach { selection ->
                    val pdfSelection = selection as? PDFSelection ?: return@forEach
                    addSelection(pdfSelection)
                }
            }
        }.recoverCatching {
            throw when (it) {
                is KPdfException -> it
                else -> KPdfException(KPdfError.Unknown(it.message ?: "Unable to search PDF text."), it)
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

    @OptIn(ExperimentalForeignApi::class)
    private fun MutableList<KPdfSearchResult>.addSelection(
        selection: PDFSelection,
    ) {
        val page = selection.pages.firstOrNull() as? PDFPage ?: return
        val pageIndex = document.indexForPage(page).toInt()
        if (pageIndex !in 0 until pageCount) return
        val pageBounds = page.boundsForBox(kPDFDisplayBoxMediaBox)
        val lineBounds = selection.selectionsByLine()
            .mapNotNull { lineSelection ->
                (lineSelection as? PDFSelection)
                    ?.boundsForPage(page)
                    ?.toSearchRect(pageBounds)
            }
        val bounds = lineBounds.ifEmpty {
            listOf(selection.boundsForPage(page).toSearchRect(pageBounds))
        }

        add(
            KPdfSearchResult(
                pageIndex = pageIndex,
                matchIndexOnPage = count { it.pageIndex == pageIndex },
                textStartIndex = 0,
                bounds = bounds,
            )
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CValue<CGRect>.toSearchRect(
    pageBounds: CValue<CGRect>,
): KPdfSearchRect {
    val selection = useContents {
        RectValues(
            minX = origin.x,
            minY = origin.y,
            maxX = origin.x + size.width,
            maxY = origin.y + size.height,
        )
    }
    val page = pageBounds.useContents {
        RectValues(
            minX = origin.x,
            minY = origin.y,
            maxX = origin.x + size.width,
            maxY = origin.y + size.height,
        )
    }
    val width = (page.maxX - page.minX).coerceAtLeast(1.0)
    val height = (page.maxY - page.minY).coerceAtLeast(1.0)

    return KPdfSearchRect(
        left = ((selection.minX - page.minX) / width).toFloat().coerceIn(0f, 1f),
        top = (1.0 - ((selection.maxY - page.minY) / height)).toFloat().coerceIn(0f, 1f),
        right = ((selection.maxX - page.minX) / width).toFloat().coerceIn(0f, 1f),
        bottom = (1.0 - ((selection.minY - page.minY) / height)).toFloat().coerceIn(0f, 1f),
    )
}

private data class RectValues(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
)

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
