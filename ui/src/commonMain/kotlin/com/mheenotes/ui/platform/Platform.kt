package com.mheenotes.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook

/** First-page cover, loaded asynchronously from the thumbnail cache. */
@Composable
expect fun NotebookThumbnail(n: Notebook, modifier: Modifier)

interface Importer {
    fun pdf(folderId: Id?)
    fun notebook(folderId: Id?)
    fun share(notebookId: Id)
    fun sharePdf(notebookId: Id)
    fun openInNewWindow(notebookId: Id)
}

/** [onResult] receives the number of notebooks imported, or -1 on failure. */
@Composable
expect fun rememberImporter(onResult: (Int) -> Unit): Importer
