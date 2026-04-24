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
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentRequest
import com.mahmoud.kpdf_core.api.KPdfOpenDocumentResult
import com.mahmoud.kpdf_core.api.KPdfSource

@Composable
internal actual fun rememberPdfOpenLauncher(): PdfOpenLauncher {
    val context = LocalContext.current
    var pendingRequest by remember { mutableStateOf<KPdfOpenDocumentRequest?>(null) }
    var pendingCallback by remember { mutableStateOf<((KPdfOpenDocumentResult) -> Unit)?>(null) }
    val currentContext by rememberUpdatedState(context)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val request = pendingRequest
        val callback = pendingCallback
        pendingRequest = null
        pendingCallback = null

        if (request == null || callback == null) {
            return@rememberLauncherForActivityResult
        }

        if (uri == null) {
            callback(KPdfOpenDocumentResult.Cancelled)
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            val bytes = currentContext.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: error("Unable to read the selected PDF.")

            if (bytes.isEmpty()) {
                error("The selected PDF is empty.")
            }

            KPdfOpenDocumentResult.Success(
                source = KPdfSource.Bytes(bytes)
            )
        }.getOrElse { throwable ->
            KPdfOpenDocumentResult.Failure(
                reason = KPdfError.Unknown(
                    throwable.message ?: "Unable to open the selected PDF."
                )
            )
        }

        callback(result)
    }

    return remember(launcher) {
        object : PdfOpenLauncher {
            override fun launch(
                request: KPdfOpenDocumentRequest,
                onResult: (KPdfOpenDocumentResult) -> Unit,
            ) {
                pendingRequest = request
                pendingCallback = onResult
                launcher.launch(request.mimeTypes.toTypedArray())
            }
        }
    }
}
