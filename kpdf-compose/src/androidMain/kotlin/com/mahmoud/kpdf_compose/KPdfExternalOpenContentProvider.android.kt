package com.mahmoud.kpdf_compose

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

internal class KPdfExternalOpenContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String = KPdfExternalMimeType

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val context = requireNotNull(context) { "Context is unavailable." }
        val relativePath = uri.lastPathSegment
            ?: error("Missing file name in URI.")
        val targetFile = File(context.cacheDir, "$KPdfExternalOpenDirectory/$relativePath")
        val canonicalBase = File(context.cacheDir, KPdfExternalOpenDirectory).canonicalFile
        val canonicalTarget = targetFile.canonicalFile

        require(canonicalTarget.path.startsWith(canonicalBase.path)) {
            "Access outside the KPDF cache directory is not allowed."
        }
        require(canonicalTarget.exists()) {
            "Requested PDF does not exist."
        }

        return ParcelFileDescriptor.open(canonicalTarget, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}

internal const val KPdfExternalOpenDirectory = "kpdf-external"
internal const val KPdfExternalMimeType = "application/pdf"
