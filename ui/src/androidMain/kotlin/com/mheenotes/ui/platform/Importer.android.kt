package com.mheenotes.ui.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mheenotes.core.model.Id
import com.mheenotes.ui.AndroidContext
import kotlinx.coroutines.launch

@Composable
actual fun rememberImporter(onResult: (Int) -> Unit): Importer {
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)
    var folder: Id? = null
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { callback.value(Imports.importPdfs(uris, folder)) }
    }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { callback.value(Imports.importArchives(uris, folder)) }
    }
    return remember {
        object : Importer {
            override fun pdf(folderId: Id?) { folder = folderId; pdf.launch(arrayOf("application/pdf")) }
            override fun notebook(folderId: Id?) { folder = folderId; file.launch(arrayOf("*/*")) }
            override fun share(notebookId: Id) { scope.launch { Exports.shareNotebook(AndroidContext.app, notebookId) } }
        }
    }
}
