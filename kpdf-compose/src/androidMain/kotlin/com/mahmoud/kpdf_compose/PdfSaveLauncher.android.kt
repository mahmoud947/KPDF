package com.mahmoud.kpdf_compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.mahmoud.kpdf_core.api.KPdfError
import com.mahmoud.kpdf_core.api.KPdfSaveRequest
import com.mahmoud.kpdf_core.api.KPdfSaveResult

@Composable
internal actual fun rememberPdfSaveLauncher(): PdfSaveLauncher {
    val context = LocalContext.current
    var pendingRequest by remember { mutableStateOf<KPdfSaveRequest?>(null) }
    var pendingCallback by remember { mutableStateOf<((KPdfSaveResult) -> Unit)?>(null) }
    val currentContext by rememberUpdatedState(context)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(KPdfSaveRequest.DefaultMimeType),
    ) { uri ->
        val request = pendingRequest
        val callback = pendingCallback
        pendingRequest = null
        pendingCallback = null

        if (request == null || callback == null) {
            return@rememberLauncherForActivityResult
        }

        if (uri == null) {
            callback(KPdfSaveResult.Cancelled)
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            currentContext.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(request.bytes)
            } ?: error("Unable to open the selected destination.")

            KPdfSaveResult.Success(location = uri.toString())
        }.getOrElse { throwable ->
            KPdfSaveResult.Failure(
                reason = KPdfError.Unknown(
                    throwable.message ?: "Unable to save the PDF."
                )
            )
        }

        callback(result)
    }

    return remember(launcher) {
        object : PdfSaveLauncher {
            override fun launch(
                request: KPdfSaveRequest,
                onResult: (KPdfSaveResult) -> Unit,
            ) {
                pendingRequest = request
                pendingCallback = onResult
                launcher.launch(request.suggestedFileName.ensurePdfExtension())
            }
        }
    }
}

private fun String.ensurePdfExtension(): String =
    if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"
