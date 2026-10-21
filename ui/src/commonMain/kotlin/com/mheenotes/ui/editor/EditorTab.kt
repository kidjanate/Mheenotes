package com.mheenotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mheenotes.core.doc.Change
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.geometry.Similarity
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.nowMillis
import com.mheenotes.core.store.NotebookStore
import com.mheenotes.ui.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pages stacked vertically in document space (page units), centered horizontally. */
class PageLayout(val pages: List<Page>) {
    val gap = 28f
    val margin = 28f
    val width: Float = (pages.maxOfOrNull { it.width } ?: 595f)
    val tops = FloatArray(pages.size)
    val height: Float

    init {
        var y = margin
        pages.forEachIndexed { i, p -> tops[i] = y; y += p.height + gap }
        height = y - gap + margin
    }

    fun left(i: Int) = (width - pages[i].width) / 2
    fun rect(i: Int) = Rect(left(i), tops[i], left(i) + pages[i].width, tops[i] + pages[i].height)

    /** Index of the page at or nearest to document y. */
    fun indexAt(y: Float): Int {
        if (pages.isEmpty()) return -1
        var lo = 0; var hi = pages.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (tops[mid] - gap / 2 <= y) lo = mid else hi = mid - 1
        }
        return lo
    }

    fun indexOf(id: Id) = pages.indexOfFirst { it.id == id }
}

/** Items lifted off the page for moving, scaling or rotating. */
class Selection(
    val pageId: Id,
    val items: List<Item>,
    val bounds: Rect,
    /** The lasso that made this selection (x, y pairs in page units), for capturing the area. */
    val lasso: FloatArray? = null,
) {
    var transform by mutableStateOf(Similarity(bounds.centerX, bounds.centerY))
    val hasStrokesOnly: Boolean get() = items.all { it is com.mheenotes.core.model.Stroke }
}

/** A lassoed region with nothing selectable inside; it can still be captured as an image. */
class CaptureArea(val pageId: Id, val lasso: FloatArray)

/** A text box being typed into. [itemId] is null for a new one. */
data class TextEdit(val pageId: Id, val itemId: Id?, val x: Float, val y: Float, val width: Float, val initial: String)

/** One open notebook: document, viewport memory and editor-local state. */
class EditorTab(val notebookId: Id) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var doc by mutableStateOf<NotebookDocument?>(null)
        private set

    /** Viewport is restored per tab: document y at the top edge and zoom relative to fit. */
    var scrollY by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var currentPage by mutableIntStateOf(0)
    val viewport = ViewportState()

    var selection by mutableStateOf<Selection?>(null)
    var textEdit by mutableStateOf<TextEdit?>(null)
    var captureArea by mutableStateOf<CaptureArea?>(null)
    var pageDialog by mutableStateOf<PageDialog?>(null)
    /** Open find-in-PDF bar; null when closed. */
    var search by mutableStateOf<PdfSearchState?>(null)
    var pdfTools: PdfTools? = null
    /** Rail filter: show only bookmarked pages. */
    var bookmarksOnly by mutableStateOf(false)
    /** Set while the pen is down, so chrome can fade out of the way. */
    var writing by mutableStateOf(false)
    /** Finishes an open text box; the canvas calls it when the user taps elsewhere. */
    var finishText: (() -> Unit)? = null
    /** Live canvas for this tab; null until it is laid out. */
    var canvas: CanvasController? = null
    /** Requests from chrome to the canvas (scroll to page, fit width). */
    var scrollRequest by mutableStateOf<ScrollRequest?>(null)

    private var thumbDirty = false
    private var metaJob: Job? = null

    init {
        scope.launch {
            val d = NotebookDocument.open(NotebookStore(notebookId, AppGraph.files), scope)
            d.onChanged = { change, strokes -> onChanged(d, change, strokes) }
            doc = d
        }
    }

    private fun onChanged(d: NotebookDocument, change: Change, strokes: Int) {
        if (strokes > 0) AppGraph.library.recordStrokes(strokes)
        val first = d.pages.value.firstOrNull()?.id
        if (touches(change, first)) thumbDirty = true
        metaJob?.cancel()
        // Coalesce library index writes while the user keeps writing.
        metaJob = scope.launch {
            delay(1500)
            syncMeta()
        }
    }

    private fun touches(c: Change, pageId: Id?): Boolean = when (c) {
        is Change.Items -> c.pageId == pageId
        is Change.Pages -> true
        is Change.Batch -> c.changes.any { touches(it, pageId) }
    }

    private fun syncMeta() {
        val d = doc ?: return
        val thumb = thumbDirty
        AppGraph.library.update(notebookId) {
            it.copy(updatedAt = nowMillis(), pageCount = d.pages.value.size)
        }
        if (thumb) {
            thumbDirty = false
            scope.launch {
                if (Thumbnails.render(notebookId, d)) {
                    AppGraph.library.update(notebookId) { it.copy(thumbVersion = it.thumbVersion + 1) }
                }
            }
        }
    }

    fun onLeave() {
        metaJob?.cancel()
        if (doc != null) syncMeta()
    }

    fun close() {
        onLeave()
        val d = doc
        scope.launch {
            d?.close()
            scope.cancel()
        }
    }
}

/** Operations the chrome asks of the platform canvas. */
interface CanvasController {
    fun commitSelection()
    fun deleteSelection()
    fun replaceSelection(transform: (Item) -> Item)
    fun selectionItems(): List<Item>
    fun select(pageId: Id, items: List<Item>)
    /** Moves the selection to the page [delta] away, keeping its position on the page. */
    fun moveSelectionToPage(delta: Int)
}

/**
 * Page and lasso polygon (page units) to capture: the moved selection's lasso, the selection's
 * frame when it has no lasso, or a lassoed empty area.
 */
fun EditorTab.captureTarget(): Pair<Id, FloatArray>? {
    selection?.let { sel ->
        val t = sel.transform
        val src = sel.lasso ?: sel.bounds.let { b -> floatArrayOf(b.left, b.top, b.right, b.top, b.right, b.bottom, b.left, b.bottom) }
        val moved = FloatArray(src.size) { i ->
            val x = src[i - i % 2]; val y = src[i - i % 2 + 1]
            if (i % 2 == 0) t.mapX(x, y) else t.mapY(x, y)
        }
        return sel.pageId to boundingRect(moved)
    }
    return captureArea?.let { it.pageId to it.lasso }
}

/** Captures are rectangular: the bounding box of the lasso, as a 4-corner polygon. */
fun boundingRect(xy: FloatArray): FloatArray {
    var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
    var i = 0
    while (i < xy.size) { l = minOf(l, xy[i]); r = maxOf(r, xy[i]); t = minOf(t, xy[i + 1]); b = maxOf(b, xy[i + 1]); i += 2 }
    return floatArrayOf(l, t, r, t, r, b, l, b)
}

/** Screen bounds of a capture area, for placing its bar. */
fun EditorTab.captureAreaScreenBounds(): Rect? {
    val a = captureArea ?: return null
    var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
    var i = 0
    while (i < a.lasso.size) {
        l = minOf(l, a.lasso[i]); r = maxOf(r, a.lasso[i]); t = minOf(t, a.lasso[i + 1]); b = maxOf(b, a.lasso[i + 1]); i += 2
    }
    return Rect(viewport.screenX(a.pageId, l), viewport.screenY(a.pageId, t), viewport.screenX(a.pageId, r), viewport.screenY(a.pageId, b))
}

/** Screen-space bounding box of the selection, for placing its action bar. */
fun EditorTab.selectionScreenBounds(): Rect? {
    val sel = selection ?: return null
    val t = sel.transform
    val b = sel.bounds
    val xs = floatArrayOf(t.mapX(b.left, b.top), t.mapX(b.right, b.top), t.mapX(b.right, b.bottom), t.mapX(b.left, b.bottom))
    val ys = floatArrayOf(t.mapY(b.left, b.top), t.mapY(b.right, b.top), t.mapY(b.right, b.bottom), t.mapY(b.left, b.bottom))
    val v = viewport
    return Rect(
        v.screenX(sel.pageId, xs.min()), v.screenY(sel.pageId, ys.min()),
        v.screenX(sel.pageId, xs.max()), v.screenY(sel.pageId, ys.max()),
    )
}

sealed interface ScrollRequest {
    data class ToPage(val index: Int, val animate: Boolean = true) : ScrollRequest
    data object FitWidth : ScrollRequest
    /** Brings [rect] (page units on page [index]) into view, centring it if it is off screen. */
    data class Reveal(val index: Int, val rect: Rect) : ScrollRequest
}

/** Renders a notebook's first page into its cover file. Platform-provided. */
expect object Thumbnails {
    suspend fun render(notebookId: Id, doc: NotebookDocument): Boolean
}
