package com.mahmoud.kpdf_core.repository

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mahmoud.kpdf_core.api.KPdfDocumentRef
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfException
import com.mahmoud.kpdf_core.api.KPdfPageBitmap
import com.mahmoud.kpdf_core.image.KPlatformImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.use

actual class KPdfDocumentFactory {
    actual suspend fun open(
        filePath: String,
        documentId: String,
    ): Result<KPdfDocumentRef> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) {
                throw KPdfException(KPdfError.FileNotFound(filePath))
            }

            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            AndroidPdfDocumentRef(
                id = documentId,
                renderer = renderer,
                descriptor = descriptor,
            )
        }.recoverCatching {
            throw when (it) {
                is KPdfException -> it
                is FileNotFoundException -> KPdfException(KPdfError.FileNotFound(filePath), it)
                is IOException -> KPdfException(KPdfError.CorruptedDocument, it)
                else -> KPdfException(KPdfError.Unknown(it.message ?: "Unable to open PDF document."), it)
            }
        }
    }
}

private class AndroidPdfDocumentRef(
    override val id: String,
    private val renderer: PdfRenderer,
    private val descriptor: ParcelFileDescriptor,
) : KPdfDocumentRef {
    private val mutex = Mutex()
    private var closed = false

    override val pageCount: Int = renderer.pageCount
    override val title: String? = null

    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
    ): Result<KPdfPageBitmap> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                checkOpen()
                if (pageIndex !in 0 until pageCount) {
                    throw KPdfException(KPdfError.Unknown("Page index out of bounds: $pageIndex"))
                }

                coroutineContext.ensureActive()

                renderer.openPage(pageIndex).use { page ->
                    val target = calculateTargetSize(
                        pageWidth = page.width,
                        pageHeight = page.height,
                        targetWidth = targetWidth,
                        targetHeight = targetHeight,
                        zoom = zoom,
                    )
                    val bitmap = createBitmap(target.width, target.height,Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    KPdfPageBitmap(
                        image = KPlatformImage(bitmap),
                        width = bitmap.width,
                        height = bitmap.height,
                        pageIndex = pageIndex,
                    )
                }
            }.recoverCatching {
                throw when (it) {
                    is KPdfException -> it
                    is IOException -> KPdfException(KPdfError.CorruptedDocument, it)
                    else -> KPdfException(KPdfError.Unknown(it.message ?: "Unable to render PDF page."), it)
                }
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    private fun checkOpen() {
        if (closed) {
            throw KPdfException(KPdfError.Unknown("PDF document is already closed."))
        }
    }
}

private data class TargetSize(
    val width: Int,
    val height: Int,
)

private fun calculateTargetSize(
    pageWidth: Int,
    pageHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
    zoom: Float,
): TargetSize {
    val safeWidth = targetWidth.coerceAtLeast(1)
    val safeHeight = targetHeight.coerceAtLeast(1)
    val pageAspect = pageWidth.toFloat() / pageHeight.toFloat()
    val boxAspect = safeWidth.toFloat() / safeHeight.toFloat()
    val base = if (pageAspect > boxAspect) {
        TargetSize(
            width = safeWidth,
            height = (safeWidth / pageAspect).toInt().coerceAtLeast(1),
        )
    } else {
        TargetSize(
            width = (safeHeight * pageAspect).toInt().coerceAtLeast(1),
            height = safeHeight,
        )
    }
    return TargetSize(
        width = (base.width * zoom).toInt().coerceAtLeast(1),
        height = (base.height * zoom).toInt().coerceAtLeast(1),
    )
}