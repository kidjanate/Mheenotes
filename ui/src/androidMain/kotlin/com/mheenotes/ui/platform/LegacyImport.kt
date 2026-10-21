package com.mheenotes.ui.platform

import android.net.Uri
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook

/** One-way import of `.mynote` files from the legacy app. */
object LegacyImport {
    suspend fun import(uri: Uri, folderId: Id?): Notebook = error("Legacy import not implemented yet")
}
