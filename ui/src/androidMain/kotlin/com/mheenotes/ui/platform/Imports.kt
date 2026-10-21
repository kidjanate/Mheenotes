package com.mheenotes.ui.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.PAPER_WHITE
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PdfRef
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.newId
import com.mheenotes.core.model.nowMillis
import com.mheenotes.core.store.AppFiles
import com.mheenotes.core.store.NotebookStore
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import com.mheenotes.ui.editor.Exporter
import com.mheenotes.ui.editor.NotebookArchive
import com.mheenotes.ui.editor.PdfPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

object Imports {
    private val resolver get() = AndroidContext.app.contentResolver

    fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    /** Imports each PDF as a new notebook whose pages show the PDF underneath ink. */
    suspend fun importPdfs(uris: List<Uri>, folderId: Id?): Int {
        var count = 0
        for (uri in uris) {
            val n = runCatching { importPdf(uri, folderId) }.onFailure { it.printStackTrace() }.getOrNull() ?: return -1
            AppGraph.library.upsert(n)
            count++
        }
        return count
    }

    private suspend fun importPdf(uri: Uri, folderId: Id?): Notebook = withContext(Dispatchers.IO) {
        val id = newId()
        val assetId = newId()
        val target = File(AppGraph.files.asset(id, "$assetId.pdf"))
        target.parentFile?.mkdirs()
        resolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        val sizes = try {
            PdfPages.sizes(target)
        } catch (e: Throwable) {
            AppFiles.deleteRecursively(AppGraph.files.notebookDir(id))
            throw e
        }
        require(sizes.isNotEmpty())
        val title = displayName(uri)?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "PDF"
        val pages = sizes.mapIndexed { i, (w, h) -> Page(newId(), (i + 1).toDouble(), w, h, Template.Blank, PAPER_WHITE, PdfRef(assetId, i)) }
        val store = NotebookStore(id, AppGraph.files)
        store.putMeta("title", title)
        store.writePages(pages, emptyList())
        store.close()
        val now = nowMillis()
        Notebook(id, title, folderId, 0, now, now, now, pageCount = pages.size)
    }

    /**
     * Files shared or opened from another app. Each becomes its own notebook; PDFs are detected
     * by type or extension, anything else is tried as a notebook file.
     */
    suspend fun importShared(uris: List<Uri>, mimeType: String?): List<Notebook> = uris.mapNotNull { uri ->
        val name = displayName(uri).orEmpty()
        val type = AndroidContext.app.contentResolver.getType(uri) ?: mimeType
        runCatching {
            when {
                type == "application/pdf" || name.endsWith(".pdf", true) -> importPdf(uri, null)
                name.endsWith(".mynote", true) -> LegacyImport.import(uri, null)
                else -> importArchive(uri, null, name)
            }
        }.onFailure { it.printStackTrace() }.getOrNull()?.also { AppGraph.library.upsert(it) }
    }

    /** Imports `.mheenote` archives and legacy `.mynote` files. */
    suspend fun importArchives(uris: List<Uri>, folderId: Id?): Int {
        var count = 0
        for (uri in uris) {
            val name = displayName(uri).orEmpty()
            val n = runCatching {
                if (name.endsWith(".mynote", true)) LegacyImport.import(uri, folderId) else importArchive(uri, folderId, name)
            }.onFailure { it.printStackTrace() }.getOrNull() ?: return -1
            AppGraph.library.upsert(n)
            count++
        }
        return count
    }

    private suspend fun importArchive(uri: Uri, folderId: Id?, name: String): Notebook = withContext(Dispatchers.IO) {
        val id = newId()
        val dir = File(AppGraph.files.notebookDir(id)).apply { mkdirs() }
        var title = name.substringBeforeLast('.').ifBlank { "Notebook" }
        var sawDb = false
        try {
            ZipInputStream(resolver.openInputStream(uri)!!).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory) continue
                    if (e.name == "title.txt") { title = zip.readBytes().decodeToString().ifBlank { title }; continue }
                    val out = File(dir, e.name).canonicalFile
                    // Reject entries that would escape the notebook folder.
                    require(out.path.startsWith(dir.canonicalPath + File.separator)) { "Bad archive entry" }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                    if (e.name == "notebook.db") sawDb = true
                }
            }
            require(sawDb) { "Not a Mheenotes file" }
            val store = NotebookStore(id, AppGraph.files)
            val pages = store.pages()
            store.putMeta("title", title)
            store.close()
            require(pages.isNotEmpty())
            val now = nowMillis()
            Notebook(id, title, folderId, 0, now, now, now, pageCount = pages.size)
        } catch (e: Throwable) {
            dir.deleteRecursively()
            throw e
        }
    }
}

object Exports {
    suspend fun shareNotebook(context: Context, notebookId: Id) {
        val title = AppGraph.library.notebook(notebookId)?.title ?: "Notebook"
        val doc = AppState.shared.tabs.firstOrNull { it.notebookId == notebookId }?.doc
        val file = NotebookArchive.write(notebookId, title, doc)
        Exporter.share(context, file, "application/octet-stream")
    }
}
