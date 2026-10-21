package com.mheenotes.core.doc

import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.store.NotebookStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

sealed interface Change {
    class Items(val pageId: Id, val removed: List<Item>, val added: List<Item>) : Change
    class Pages(val before: List<Page>, val after: List<Page>) : Change
    class Batch(val changes: List<Change>) : Change
}

/**
 * An open notebook. Page items load lazily and decode off the main thread; edits apply to
 * memory immediately and persist through the store's ordered writer.
 *
 * All public mutators must be called from the main thread.
 */
class NotebookDocument private constructor(
    val store: NotebookStore,
    private val scope: CoroutineScope,
    initialPages: List<Page>,
    private var seq: Long,
) {
    private val _pages = MutableStateFlow(initialPages)
    val pages: StateFlow<List<Page>> = _pages

    private val contents = HashMap<Id, PageContent>()
    private val loading = HashMap<Id, Deferred<PageContent>>()

    /** Bumps on every content change; renderers observe it to repaint. */
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    private val undoStack = ArrayDeque<Change>()
    private val redoStack = ArrayDeque<Change>()
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    val canRedo: StateFlow<Boolean> = _canRedo

    /** Called after any recorded change with the number of strokes added (for activity stats). */
    var onChanged: (Change, strokesAdded: Int) -> Unit = { _, _ -> }

    /** Called whenever in-memory items change, so renderers can repaint just those areas. */
    var onContent: (pageId: Id, removed: List<Item>, added: List<Item>) -> Unit = { _, _, _ -> }

    fun nextSeq(): Long = ++seq

    fun content(pageId: Id): PageContent? = contents[pageId]

    fun page(pageId: Id): Page? = _pages.value.firstOrNull { it.id == pageId }

    suspend fun load(pageId: Id): PageContent {
        contents[pageId]?.let { return it }
        val job = loading.getOrPut(pageId) {
            scope.async {
                val items = store.items(pageId)
                withContext(Dispatchers.Default) { PageContent(items) }
            }
        }
        val c = job.await()
        loading.remove(pageId)
        return contents.getOrPut(pageId) { c }.also { _revision.value++ }
    }

    /** Keeps only [keep] in memory; everything else reloads on demand. */
    fun retainOnly(keep: Set<Id>) {
        contents.keys.retainAll(keep)
    }

    // ---- Edits ----------------------------------------------------------------------------

    fun edit(pageId: Id, removed: List<Item>, added: List<Item>, record: Boolean = true) {
        if (removed.isEmpty() && added.isEmpty()) return
        val change = Change.Items(pageId, removed, added)
        applyItems(change, forward = true)
        if (record) push(change)
    }

    fun editPages(after: List<Page>, record: Boolean = true) {
        val change = Change.Pages(_pages.value, after.sortedBy { it.ord })
        applyPages(change.before, change.after)
        if (record) push(change)
    }

    /**
     * Memory-only change for live feedback (erasing as the pen moves, lifting a selection).
     * Nothing is persisted or recorded until [commit].
     */
    fun preview(pageId: Id, removed: List<Item>, added: List<Item>) {
        if (removed.isEmpty() && added.isEmpty()) return
        contents[pageId]?.apply(removed, added)
        _revision.value++
        onContent(pageId, removed, added)
    }

    /** Persists and records a change whose memory effect was already applied by [preview]. */
    fun commit(pageId: Id, removed: List<Item>, added: List<Item>) {
        if (removed.isEmpty() && added.isEmpty()) return
        val change = Change.Items(pageId, removed, added)
        store.writeItems(pageId, removed.map { it.id }, added)
        push(change)
        onChanged(change, added.count { it is Stroke })
    }

    /** Groups several changes into one undo step. */
    fun batch(changes: List<Change>) {
        if (changes.isEmpty()) return
        val b = Change.Batch(changes)
        apply(b, forward = true)
        push(b)
    }

    suspend fun undo() {
        val c = undoStack.removeLastOrNull() ?: return
        ensureLoaded(c)
        apply(c, forward = false)
        redoStack.addLast(c)
        syncFlags()
    }

    suspend fun redo() {
        val c = redoStack.removeLastOrNull() ?: return
        ensureLoaded(c)
        apply(c, forward = true)
        undoStack.addLast(c)
        syncFlags()
    }

    private suspend fun ensureLoaded(c: Change) {
        when (c) {
            is Change.Items -> load(c.pageId)
            is Change.Batch -> c.changes.forEach { ensureLoaded(it) }
            is Change.Pages -> Unit
        }
    }

    private fun push(c: Change) {
        undoStack.addLast(c)
        if (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        redoStack.clear()
        syncFlags()
    }

    private fun syncFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun apply(c: Change, forward: Boolean) {
        when (c) {
            is Change.Items -> applyItems(c, forward)
            is Change.Pages -> if (forward) applyPages(c.before, c.after) else applyPages(c.after, c.before)
            is Change.Batch -> (if (forward) c.changes else c.changes.asReversed()).forEach { apply(it, forward) }
        }
    }

    private fun applyItems(c: Change.Items, forward: Boolean) {
        val removed = if (forward) c.removed else c.added
        val added = if (forward) c.added else c.removed
        contents[c.pageId]?.apply(removed, added)
        store.writeItems(c.pageId, removed.map { it.id }, added)
        _revision.value++
        onContent(c.pageId, removed, added)
        onChanged(c, if (forward) added.count { it is Stroke } else 0)
    }

    private fun applyPages(from: List<Page>, to: List<Page>) {
        val toIds = to.mapTo(HashSet()) { it.id }
        val fromById = from.associateBy { it.id }
        store.writePages(
            upserted = to.filter { fromById[it.id] != it },
            deleted = from.filter { it.id !in toIds }.map { it.id },
        )
        _pages.value = to
        _revision.value++
        onChanged(Change.Pages(from, to), 0)
    }

    suspend fun flush() = store.flush()

    suspend fun close() {
        store.flush()
        store.close()
    }

    companion object {
        const val HISTORY_LIMIT = 300

        suspend fun open(store: NotebookStore, scope: CoroutineScope): NotebookDocument {
            val pages = store.pages()
            return NotebookDocument(store, scope, pages, store.maxSeq())
        }
    }
}
