package com.mheenotes.core.doc

import com.mheenotes.core.geometry.ItemDrawOrder
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import kotlin.concurrent.Volatile
import kotlin.math.floor

/**
 * Immutable view of one page's items, safe to read from render threads.
 *
 * Items are bucketed into a uniform grid. Edits copy only the top-level map and the touched
 * cells, so appending a stroke to a 20,000-stroke page stays cheap.
 */
class PageSnapshot internal constructor(
    val version: Long,
    internal val cells: Map<Long, List<Item>>,
    /** Every item in draw order. */
    val items: List<Item>,
) {
    /** Items whose bounds touch [area], in draw order. */
    fun query(area: Rect): List<Item> {
        if (items.isEmpty()) return emptyList()
        // Small pages or huge areas: scanning the ordered list is cheaper than merging cells.
        val x0 = cell(area.left); val x1 = cell(area.right)
        val y0 = cell(area.top); val y1 = cell(area.bottom)
        if ((x1 - x0 + 1L) * (y1 - y0 + 1L) > 48) return items.filter { it.bounds.intersects(area) }
        val seen = HashSet<Id>()
        val out = ArrayList<Item>()
        for (cy in y0..y1) for (cx in x0..x1) {
            val list = cells[key(cx, cy)] ?: continue
            for (item in list) if (item.bounds.intersects(area) && seen.add(item.id)) out += item
        }
        out.sortWith(ItemDrawOrder)
        return out
    }

    companion object {
        val Empty = PageSnapshot(0, emptyMap(), emptyList())
    }
}

private const val CELL = 64f

private fun cell(v: Float): Int = floor(v / CELL).toInt()
private fun key(cx: Int, cy: Int): Long = (cx.toLong() shl 32) xor (cy.toLong() and 0xffffffffL)

private inline fun forCells(r: Rect, block: (Long) -> Unit) {
    for (cy in cell(r.top)..cell(r.bottom)) for (cx in cell(r.left)..cell(r.right)) block(key(cx, cy))
}

/**
 * Mutable owner of a page's items. Mutated on one thread (the UI thread); publishes immutable
 * [snapshot]s that render threads read without locks.
 */
class PageContent(initial: List<Item> = emptyList()) {
    private val byId = HashMap<Id, Item>()

    @Volatile
    var snapshot: PageSnapshot = PageSnapshot.Empty
        private set

    init {
        if (initial.isNotEmpty()) {
            val cells = HashMap<Long, MutableList<Item>>()
            for (item in initial) {
                byId[item.id] = item
                forCells(item.bounds) { k -> cells.getOrPut(k) { ArrayList() }.add(item) }
            }
            snapshot = PageSnapshot(1, cells, initial.sortedWith(ItemDrawOrder))
        }
    }

    val maxSeq: Long get() = snapshot.items.maxOfOrNull { it.seq } ?: 0L

    operator fun get(id: Id): Item? = byId[id]

    fun apply(removed: Collection<Item>, added: Collection<Item>) {
        if (removed.isEmpty() && added.isEmpty()) return
        val old = snapshot
        val cells = HashMap(old.cells)
        val touched = HashMap<Long, MutableList<Item>>()
        fun cellList(k: Long): MutableList<Item> =
            touched.getOrPut(k) { ArrayList(cells[k] ?: emptyList()) }

        val removedIds = HashSet<Id>()
        for (item in removed) {
            val existing = byId.remove(item.id) ?: continue
            removedIds += existing.id
            forCells(existing.bounds) { k -> cellList(k).removeAll { it.id == existing.id } }
        }
        for (item in added) {
            byId.put(item.id, item)?.let { prior ->
                removedIds += prior.id
                forCells(prior.bounds) { k -> cellList(k).removeAll { it.id == prior.id } }
            }
            forCells(item.bounds) { k -> cellList(k).add(item) }
        }
        for ((k, list) in touched) if (list.isEmpty()) cells.remove(k) else cells[k] = list

        var items = if (removedIds.isEmpty()) old.items else old.items.filter { it.id !in removedIds }
        if (added.isNotEmpty()) {
            val sorted = added.sortedWith(ItemDrawOrder)
            val last = items.lastOrNull()
            // Appends on top of the stack are the common case and keep the list sorted for free.
            items = if (last == null || ItemDrawOrder.compare(last, sorted.first()) < 0) items + sorted
            else (items + sorted).sortedWith(ItemDrawOrder)
        }
        snapshot = PageSnapshot(old.version + 1, cells, items)
    }

}
