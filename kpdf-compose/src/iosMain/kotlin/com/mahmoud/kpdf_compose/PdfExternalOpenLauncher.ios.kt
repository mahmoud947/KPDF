package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfExternalOpenRequest
import com.mahmoud.kpdf_core.api.KPdfExternalOpenResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun rememberPdfExternalOpenLauncher(): PdfExternalOpenLauncher {
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        object : PdfExternalOpenLauncher {
            private var activeDelegate: PdfExternalOpenDelegate? = null
            private var activeController: UIDocumentInteractionController? = null

            override fun launch(
                request: KPdfExternalOpenRequest,
                onResult: (KPdfExternalOpenResult) -> Unit,
            ) {
                val tempPath = buildTempExternalOpenPath(request.suggestedFileName.ensurePdfExtension())
                val didCreateTempFile = NSFileManager.defaultManager.createFileAtPath(
                    path = tempPath,
                    contents = request.bytes.toNSData(),
                    attributes = null,
                )

                if (!didCreateTempFile) {
                    onResult(
                        KPdfExternalOpenResult.Failure(
                            reason = KPdfError.Unknown("Unable to prepare the PDF for external opening.")
                        )
                    )
                    return
                }

                val fileUrl = NSURL.fileURLWithPath(tempPath)
                val interactionController = UIDocumentInteractionController.interactionControllerWithURL(fileUrl)
                val delegate = PdfExternalOpenDelegate(
                    viewController = viewController,
                    tempPath = tempPath,
                    onFinish = { result ->
                        activeDelegate = null
                        activeController = null
                        onResult(result)
                    },
                )

                activeDelegate = delegate
                activeController = interactionController
                interactionController.UTI = "com.adobe.pdf"
                interactionController.delegate = delegate

                val didPresent = interactionController.presentOpenInMenuFromRect(
                    rect = viewController.view.bounds,
                    inView = viewController.view,
                    animated = true,
                )

                if (!didPresent) {
                    activeDelegate = null
                    activeController = null
                    NSFileManager.defaultManager.removeItemAtPath(tempPath, error = null)
                    onResult(
                        KPdfExternalOpenResult.Failure(
                            reason = KPdfError.Unknown("No app was found that can open PDF files.")
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PdfExternalOpenDelegate(
    private val viewController: platform.UIKit.UIViewController,
    private val tempPath: String,
    private val onFinish: (KPdfExternalOpenResult) -> Unit,
) : NSObject(), UIDocumentInteractionControllerDelegateProtocol {

    override fun documentInteractionControllerViewControllerForPreview(
        controller: UIDocumentInteractionController,
    ) = viewController

    override fun documentInteractionControllerDidDismissOpenInMenu(
        controller: UIDocumentInteractionController,
    ) {
        onFinish(
            KPdfExternalOpenResult.Success(location = controller.URL?.absoluteString)
        )
        NSFileManager.defaultManager.removeItemAtPath(tempPath, error = null)
    }
}

private fun String.ensurePdfExtension(): String =
    if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"

private fun buildTempExternalOpenPath(fileName: String): String =
    NSTemporaryDirectory().trimEnd('/') + "/" + NSUUID.UUID().UUIDString + "-" + fileName

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned {
            NSData.create(
                bytes = it.addressOf(0),
                length = size.toULong(),
            )
        }
    }
