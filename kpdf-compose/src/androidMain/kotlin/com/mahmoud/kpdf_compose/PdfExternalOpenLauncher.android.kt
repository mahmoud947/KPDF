package com.mahmoud.kpdf_compose

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfExternalOpenRequest
import com.mahmoud.kpdf_core.api.KPdfExternalOpenResult
import java.io.File

@Composable
internal actual fun rememberPdfExternalOpenLauncher(): PdfExternalOpenLauncher {
    val context = LocalContext.current
    val currentContext by rememberUpdatedState(context)

    return remember {
        object : PdfExternalOpenLauncher {
            override fun launch(
                request: KPdfExternalOpenRequest,
                onResult: (KPdfExternalOpenResult) -> Unit,
            ) {
                val result = runCatching {
                    val file = writeExternalOpenPdf(
                        cacheDir = currentContext.cacheDir,
                        fileName = request.suggestedFileName.ensurePdfExtension(),
                        bytes = request.bytes,
                    )
                    val fileUri = buildExternalOpenUri(
                        packageName = currentContext.packageName,
                        fileName = file.name,
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, request.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    currentContext.startActivity(intent)
                    KPdfExternalOpenResult.Success(location = fileUri.toString())
                }.getOrElse { throwable ->
                    when (throwable) {
                        is ActivityNotFoundException -> {
                            KPdfExternalOpenResult.Failure(
                                reason = KPdfError.Unknown("No app was found that can open PDF files.")
                            )
                        }

                        else -> {
                            KPdfExternalOpenResult.Failure(
                                reason = KPdfError.Unknown(
                                    throwable.message ?: "Unable to open the PDF in an external app."
                                )
                            )
                        }
                    }
                }

                onResult(result)
            }
        }
    }
}

private fun writeExternalOpenPdf(
    cacheDir: File,
    fileName: String,
    bytes: ByteArray,
): File {
    val directory = File(cacheDir, KPdfExternalOpenDirectory).apply { mkdirs() }
    return File(directory, fileName).apply {
        writeBytes(bytes)
    }
}

private fun buildExternalOpenUri(
    packageName: String,
    fileName: String,
): Uri = Uri.parse("content://$packageName.kpdf.provider/$fileName")

private fun String.ensurePdfExtension(): String =
    if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"
