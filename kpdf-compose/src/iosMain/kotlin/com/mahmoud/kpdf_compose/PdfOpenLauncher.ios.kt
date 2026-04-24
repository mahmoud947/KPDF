package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentRequest
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentResult
import com.mahmoud.kpdf_core.api.KPdfSource
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun rememberPdfOpenLauncher(): PdfOpenLauncher {
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        object : PdfOpenLauncher {
            private var activeDelegate: PdfDocumentPickerDelegate? = null

            override fun launch(
                request: KPdfOpenDocumentRequest,
                onResult: (KPdfOpenDocumentResult) -> Unit,
            ) {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypePDF),
                    asCopy = true,
                )

                val delegate = PdfDocumentPickerDelegate(
                    onResult = { result ->
                        activeDelegate = null
                        onResult(result)
                    },
                )

                activeDelegate = delegate
                picker.delegate = delegate
                picker.allowsMultipleSelection = false
                picker.shouldShowFileExtensions = true

                viewController.presentViewController(
                    viewControllerToPresent = picker,
                    animated = true,
                    completion = null,
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PdfDocumentPickerDelegate(
    private val onResult: (KPdfOpenDocumentResult) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(KPdfOpenDocumentResult.Cancelled)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onResult(
                KPdfOpenDocumentResult.Failure(
                    reason = KPdfError.Unknown("No PDF was selected.")
                )
            )
            return
        }

        val data = NSData.dataWithContentsOfURL(url)
        if (data == null) {
            onResult(
                KPdfOpenDocumentResult.Failure(
                    reason = KPdfError.Unknown("Unable to read the selected PDF.")
                )
            )
            return
        }

        val bytes = data.toByteArray()
        if (bytes.isEmpty()) {
            onResult(
                KPdfOpenDocumentResult.Failure(
                    reason = KPdfError.Unknown("The selected PDF is empty.")
                )
            )
            return
        }

        onResult(
            KPdfOpenDocumentResult.Success(
                source = KPdfSource.Bytes(bytes)
            )
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isEmpty()) return bytes

    bytes.usePinned { pinned ->
        memcpy(
            pinned.addressOf(0),
            this.bytes,
            length,
        )
    }
    return bytes
}
