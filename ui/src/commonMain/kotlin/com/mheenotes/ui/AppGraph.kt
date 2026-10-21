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
        library.state.value.folders.filter { (it.trashedAt ?: Long.MAX_VALUE) < cutoff && isTrashRoot(it) }.forEach { deleteFolderForever(it.id) }
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
            if (n.trashedAt == null) return@update n
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
        val f = Folder(newId(), uniqueFolderName(name, parentId), parentId, nowMillis())
        library.upsertFolder(f)
        return f
    }

    fun renameFolder(id: Id, name: String) {
        library.state.value.folders.firstOrNull { it.id == id }?.let { library.upsertFolder(it.copy(name = uniqueFolderName(name, it.parentId, except = id))) }
    }

    /** "Name", or "Name (2)", "Name (3)"… when a sibling already uses it. */
    fun uniqueFolderName(name: String, parentId: Id?, except: Id? = null): String {
        val taken = library.state.value.folders.filter { it.parentId == parentId && it.trashedAt == null && it.id != except }.map { it.name.lowercase() }.toSet()
        if (name.lowercase() !in taken) return name
        var n = 2
        while ("$name ($n)".lowercase() in taken) n++
        return "$name ($n)"
    }

    private fun subtree(id: Id): Set<Id> {
        val folders = library.state.value.folders
        return HashSet<Id>().apply {
            fun collect(f: Id) { add(f); folders.filter { it.parentId == f }.forEach { collect(it.id) } }
            collect(id)
        }
    }

    /** A trashed folder whose parent was not trashed in the same operation (shown in Trash). */
    fun isTrashRoot(f: Folder): Boolean {
        val t = f.trashedAt ?: return false
        val parent = library.state.value.folders.firstOrNull { it.id == f.parentId }
        return parent == null || parent.trashedAt != t
    }

    /**
     * Moves a folder, its subfolders and their active notebooks to Trash as one unit. The shared
     * timestamp marks what this operation trashed, so restore brings back exactly that.
     */
    fun deleteFolder(id: Id) {
        val now = nowMillis()
        val tree = subtree(id)
        val s = library.state.value
        s.folders.filter { it.id in tree && it.trashedAt == null }.forEach { library.upsertFolder(it.copy(trashedAt = now)) }
        s.notebooks.filter { it.folderId in tree && it.trashedAt == null }.forEach { n -> library.upsert(n.copy(trashedAt = now)) }
    }

    fun restoreFolder(id: Id) {
        val s = library.state.value
        val root = s.folders.firstOrNull { it.id == id } ?: return
        val t = root.trashedAt ?: return
        val tree = subtree(id)
        val parentAlive = root.parentId == null || s.folders.any { it.id == root.parentId && it.trashedAt == null }
        s.folders.filter { it.id in tree && it.trashedAt == t }.forEach { f ->
            library.upsertFolder(f.copy(trashedAt = null, parentId = if (f.id == id && !parentAlive) null else f.parentId,
                name = if (f.id == id) uniqueFolderName(f.name, if (parentAlive) f.parentId else null, except = f.id) else f.name))
        }
        // Notebooks trashed on their own before the folder stay in Trash.
        s.notebooks.filter { it.folderId in tree && it.trashedAt == t }.forEach { library.upsert(it.copy(trashedAt = null)) }
    }

    fun deleteFolderForever(id: Id) {
        val tree = subtree(id)
        val s = library.state.value
        s.notebooks.filter { it.folderId in tree && it.trashedAt != null }.forEach { deleteForever(it.id) }
        // Notebooks restored or never trashed (should not happen) move to the top level.
        s.notebooks.filter { it.folderId in tree && it.trashedAt == null }.forEach { library.upsert(it.copy(folderId = null)) }
        tree.forEach { library.deleteFolderForever(it) }
    }

    /** Moves a folder under [parentId] (null = top level), refusing to move it into itself. */
    fun moveFolder(id: Id, parentId: Id?) {
        if (parentId != null && parentId in subtree(id)) return
        library.state.value.folders.firstOrNull { it.id == id }?.let { f ->
            library.upsertFolder(f.copy(parentId = parentId, name = uniqueFolderName(f.name, parentId, except = id)))
        }
    }

    /** "Parent / Child" path of a folder, for search. */
    fun folderPath(id: Id?): String {
        val folders = library.state.value.folders
        val parts = ArrayList<String>()
        var cur = id
        while (cur != null) {
            val f = folders.firstOrNull { it.id == cur } ?: break
            parts.add(0, f.name)
            cur = f.parentId
        }
        return parts.joinToString(" / ")
    }

    const val TRASH_DAYS = 30
}

/** Expected platform helpers that need OS APIs. */
expect object Platform {
    fun copyDirectory(from: String, to: String)
    fun copyFile(from: String, to: String)
}
