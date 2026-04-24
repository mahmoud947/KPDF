package com.mahmoud.kpdf_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfSaveRequest
import com.mahmoud.kpdf_core.api.KPdfSaveResult
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
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIModalPresentationFormSheet

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun rememberPdfSaveLauncher(): PdfSaveLauncher {
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        object : PdfSaveLauncher {
            override fun launch(
                request: KPdfSaveRequest,
                onResult: (KPdfSaveResult) -> Unit,
            ) {
                val tempPath = buildTempPdfPath(request.suggestedFileName.ensurePdfExtension())
                val didCreateTempFile = NSFileManager.defaultManager.createFileAtPath(
                    path = tempPath,
                    contents = request.bytes.toNSData(),
                    attributes = null,
                )

                if (!didCreateTempFile) {
                    onResult(
                        KPdfSaveResult.Failure(
                            reason = KPdfError.Unknown("Unable to prepare the PDF for export.")
                        )
                    )
                    return
                }

                val fileUrl = NSURL.fileURLWithPath(tempPath)
                val activityController = UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null,
                )
                activityController.modalPresentationStyle = UIModalPresentationFormSheet

                activityController.completionWithItemsHandler = { _, completed, _, error ->
                    NSFileManager.defaultManager.removeItemAtPath(tempPath, error = null)

                    when {
                        error != null -> {
                            onResult(
                                KPdfSaveResult.Failure(
                                    reason = KPdfError.Unknown(
                                        error.localizedDescription
                                    )
                                )
                            )
                        }

                        completed -> {
                            onResult(
                                KPdfSaveResult.Success(location = fileUrl.absoluteString)
                            )
                        }

                        else -> {
                            onResult(KPdfSaveResult.Cancelled)
                        }
                    }
                }

                viewController.presentViewController(
                    viewControllerToPresent = activityController,
                    animated = true,
                    completion = null,
                )
            }
        }
    }
}

private fun String.ensurePdfExtension(): String =
    if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"

private fun buildTempPdfPath(fileName: String): String =
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
