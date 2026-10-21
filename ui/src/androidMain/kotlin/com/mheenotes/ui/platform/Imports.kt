package com.mheenotes.ui.platform

import android.content.Context
import android.net.Uri
import com.mheenotes.core.model.Id

object Imports {
    suspend fun importPdfs(uris: List<Uri>, folderId: Id?): Int = -1
    suspend fun importArchives(uris: List<Uri>, folderId: Id?): Int = -1
}

object Exports {
    suspend fun shareNotebook(context: Context, notebookId: Id) {}
}
