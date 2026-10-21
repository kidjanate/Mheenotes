package com.mheenotes.ui

import com.mheenotes.core.model.Folder
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.PAPER_WHITE
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.newId
import com.mheenotes.core.model.nowMillis
import com.mheenotes.core.store.AppFiles
import com.mheenotes.core.store.Library
import com.mheenotes.core.store.NotebookStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Process-wide singletons. Initialised once by the platform entry point. */
object AppGraph {
    lateinit var files: AppFiles
        private set
    lateinit var library: Library
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings = Settings()

    private var initialized = false
    private val loaded = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** Suspends until the library index is in memory (for widgets and other headless callers). */
    suspend fun awaitLoaded() = loaded.await()

    fun init(root: String) {
        if (initialized) return
        initialized = true
        files = AppFiles(root)
        library = Library(files)
        scope.launch {
            library.load()
            settings.load(library)
            loaded.complete(Unit)
            purgeOldTrash()
        }
    }

    private fun purgeOldTrash() {
        val cutoff = nowMillis() - TRASH_DAYS * 86_400_000L
        library.state.value.notebooks.filter { (it.trashedAt ?: Long.MAX_VALUE) < cutoff }.forEach { deleteForever(it.id) }
    }

    // ---- Notebook lifecycle ----------------------------------------------------------------

    fun createNotebook(
        title: String,
        folderId: Id?,
        template: Template,
        size: PageSize,
        paperArgb: Int = PAPER_WHITE,
    ): Notebook {
        val now = nowMillis()
        val n = Notebook(newId(), title, folderId, 0, now, now, now)
        val store = NotebookStore(n.id, files)
        store.putMeta("title", title)
        store.writePages(listOf(Page(newId(), 1.0, size.width, size.height, template, paperArgb)), emptyList())
        io.launch { store.close() }
        library.upsert(n)
        return n
    }

    fun trash(ids: Collection<Id>) = ids.forEach { id -> library.update(id) { it.copy(trashedAt = nowMillis()) } }

    fun restore(ids: Collection<Id>) = ids.forEach { id ->
        library.update(id) { n ->
            // A notebook whose folder was deleted comes back at the top level.
            val folderAlive = n.folderId == null || library.state.value.folders.any { it.id == n.folderId && it.trashedAt == null }
            n.copy(trashedAt = null, folderId = if (folderAlive) n.folderId else null)
        }
    }

    fun deleteForever(id: Id) {
        library.deleteForever(id)
        io.launch {
            AppFiles.deleteRecursively(files.notebookDir(id))
            AppFiles.deleteRecursively(files.thumb(id))
        }
    }

    fun setFavorite(ids: Collection<Id>, favorite: Boolean) = ids.forEach { id -> library.update(id) { it.copy(favorite = favorite) } }

    fun move(ids: Collection<Id>, folderId: Id?) = ids.forEach { id -> library.update(id) { it.copy(folderId = folderId, updatedAt = nowMillis()) } }

    fun rename(id: Id, title: String) = library.update(id) { it.copy(title = title.ifBlank { it.title }) }

    fun duplicate(id: Id, copySuffix: String) {
        val src = library.notebook(id) ?: return
        io.launch {
            val copy = src.copy(id = newId(), title = "${src.title} $copySuffix", createdAt = nowMillis(), updatedAt = nowMillis(), openedAt = nowMillis(), favorite = false)
            Platform.copyDirectory(files.notebookDir(src.id), files.notebookDir(copy.id))
            Platform.copyFile(files.thumb(src.id), files.thumb(copy.id))
            scope.launch { library.upsert(copy) }
        }
    }

    fun createFolder(name: String, parentId: Id?): Folder {
        val f = Folder(newId(), name, parentId, nowMillis())
        library.upsertFolder(f)
        return f
    }

    fun renameFolder(id: Id, name: String) {
        library.state.value.folders.firstOrNull { it.id == id }?.let { library.upsertFolder(it.copy(name = name)) }
    }

    /** Deletes a folder and its subfolders; their notebooks go to Trash. */
    fun deleteFolder(id: Id) {
        val s = library.state.value
        val doomed = HashSet<Id>().apply {
            fun collect(f: Id) { add(f); s.folders.filter { it.parentId == f }.forEach { collect(it.id) } }
            collect(id)
        }
        trash(s.notebooks.filter { it.folderId in doomed && it.trashedAt == null }.map { it.id })
        doomed.forEach { library.deleteFolderForever(it) }
    }

    const val TRASH_DAYS = 30
}

/** Expected platform helpers that need OS APIs. */
expect object Platform {
    fun copyDirectory(from: String, to: String)
    fun copyFile(from: String, to: String)
}
