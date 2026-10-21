package com.mheenotes.ui.editor

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.mheenotes.core.doc.PageContent
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.Stroke
import com.mheenotes.ui.AppGraph
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

const val TILE_PX = 512

/** Render threads; leaves cores for the UI and ink threads. */
private val WORKERS = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4)

/**
 * Zoom bucket: tiles at level L are rasterized at 2^L px per page unit. Always at or above the
 * screen scale, so finished ink is only ever scaled down (sharp, and close to the live stroke).
 */
fun levelFor(scale: Float): Int = kotlin.math.ceil(ln(scale.toDouble()) / ln(2.0) - 0.05).toInt()

fun levelScale(level: Int): Float = 2.0.pow(level).toFloat()

/** Page units covered by one tile edge at [level]. */
fun tileSpan(level: Int): Float = TILE_PX / levelScale(level)

data class TileKey(val pageId: Id, val level: Int, val tx: Int, val ty: Int)

class Tile(val key: TileKey) {
    @Volatile var bitmap: Bitmap? = null
    /** Incremented when content under the tile changes. */
    @Volatile var epoch = 0
    @Volatile var renderedEpoch = -1
    @Volatile var queued = false
    /** The pending render and its priority; a more urgent request supersedes it. */
    @Volatile var job: Any? = null
    var queuedPriority = 0f
    /** Frame in which the tile was last needed; stale jobs are skipped. */
    @Volatile var wantedFrame = 0L
    var lastDrawn = 0L

    val fresh: Boolean get() = bitmap != null && renderedEpoch == epoch

    fun rect(): Rect {
        val span = tileSpan(key.level)
        return Rect(key.tx * span, key.ty * span, (key.tx + 1) * span, (key.ty + 1) * span)
    }
}

/**
 * Rasterized page tiles with an LRU byte budget. Render jobs run on a small pool, newest
 * and closest-to-center first. All map mutations happen on the main thread.
 */
class TileCache(
    private val notebookId: Id,
    private val rasterizer: Rasterizer,
    private val onReady: () -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val tiles = HashMap<TileKey, Tile>()
    private val free = ArrayDeque<Bitmap>()
    private val retired = ArrayDeque<Pair<Long, Bitmap>>()
    val frame = AtomicLong(0)
    /** Tiles that fit in the user's render memory setting (each tile is 1 MB). */
    private val budget: Int get() = AppGraph.settings.renderMemory.megabytes * 1024 * 1024 / (TILE_PX * TILE_PX * 4)
    private var closed = false

    private val pool = ThreadPoolExecutor(
        WORKERS, WORKERS, 30, TimeUnit.SECONDS, PriorityBlockingQueue(),
    ) { r -> Thread(r, "mheenotes-tiles").apply { priority = Thread.NORM_PRIORITY - 1; isDaemon = true } }

    private inner class Job(val tile: Tile, val page: Page, val content: PageContent, val priority: Float, val seq: Long) :
        Runnable, Comparable<Job> {
        override fun compareTo(other: Job): Int = compareValuesBy(this, other, { it.priority }, { -it.seq })

        override fun run() {
            val t = tile
            if (t.job !== this) return // superseded by a more urgent request
            if (closed || t.wantedFrame < frame.get() - 3) { t.queued = false; t.job = null; return }
            val epoch = t.epoch
            val bmp = obtain()
            val span = tileSpan(t.key.level)
            val area = t.rect()
            try {
                // Animated strokes move every frame, so the canvas draws them live instead.
                val items = content.snapshot.query(area.outset(span * 0.01f)).let { all ->
                    if (all.any { it is Stroke && it.isAnimated }) all.filterNot { it is Stroke && it.isAnimated } else all
                }
                rasterizer.render(bmp, notebookId, page, items, levelScale(t.key.level), area.left, area.top)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            main.post {
                if (t.job === this) { t.queued = false; t.job = null }
                if (closed) { recycle(bmp); return@post }
                t.bitmap?.let { retire(it) }
                t.bitmap = bmp
                t.renderedEpoch = epoch
                if (tiles[t.key] !== t) retire(bmp)
                onReady()
            }
        }
    }

    private val jobSeq = AtomicLong(0)

    /** At the memory budget: prefetching ahead of the view would only evict what is on screen. */
    var full = false
        private set

    fun get(key: TileKey): Tile? = tiles[key]

    fun getOrCreate(key: TileKey): Tile = tiles.getOrPut(key) { Tile(key) }

    /** Queues [tile] if it is missing or stale. Lower [priority] renders sooner. */
    fun request(tile: Tile, page: Page, content: PageContent, priority: Float) {
        tile.wantedFrame = frame.get()
        if (tile.fresh) return
        // A tile queued as a low-priority prefetch must not wait behind other prefetches once it is on screen.
        if (tile.queued && priority >= tile.queuedPriority - 5e5f) return
        tile.queued = true
        tile.queuedPriority = priority
        val job = Job(tile, page, content, priority, jobSeq.incrementAndGet())
        tile.job = job
        pool.execute(job)
    }

    /** Marks tiles touching [area] (page units) stale; they keep showing until re-rendered. */
    fun invalidate(pageId: Id, area: Rect) {
        for (t in tiles.values) if (t.key.pageId == pageId && t.rect().intersects(area)) t.epoch++
    }

    fun invalidatePage(pageId: Id) {
        for (t in tiles.values) if (t.key.pageId == pageId) t.epoch++
    }

    fun dropPage(pageId: Id) {
        val doomed = tiles.values.filter { it.key.pageId == pageId }
        doomed.forEach { t -> tiles.remove(t.key); t.bitmap?.let { retire(it) } }
    }

    /** Call once per drawn frame: recycles retired bitmaps and enforces the budget. */
    fun endFrame() {
        val f = frame.incrementAndGet()
        while (retired.isNotEmpty() && retired.first().first < f - 2) recycle(retired.removeFirst().second)
        val withBitmaps = tiles.values.count { it.bitmap != null }
        full = withBitmaps >= budget
        if (withBitmaps > budget) {
            // Recency counts prefetch requests too: a tile rendered ahead of the scroll but not yet drawn
            // must not be the first thing evicted, or it is re-rendered and evicted again forever.
            val victims = tiles.values.filter { it.bitmap != null && maxOf(it.lastDrawn, it.wantedFrame) < f - 1 }
                .sortedBy { maxOf(it.lastDrawn, it.wantedFrame) }
                .take(withBitmaps - budget)
            for (t in victims) {
                if (t.queued) continue
                tiles.remove(t.key)
                t.bitmap?.let { retire(it) }
                t.bitmap = null
            }
        }
        // Forget empty entries that nobody wants any more.
        if (tiles.size > budget * 4) tiles.values.removeAll { it.bitmap == null && !it.queued && it.wantedFrame < f - 30 }
    }

    private fun retire(b: Bitmap) = retired.addLast(frame.get() to b)

    private fun obtain(): Bitmap = synchronized(free) { free.removeLastOrNull() }
        ?: Bitmap.createBitmap(TILE_PX, TILE_PX, Bitmap.Config.ARGB_8888)

    private fun recycle(b: Bitmap) = synchronized(free) {
        if (free.size < 24) free.addLast(b) else b.recycle()
    }

    fun close() {
        closed = true
        pool.shutdownNow()
        tiles.values.forEach { it.bitmap = null }
        tiles.clear()
        retired.clear()
        synchronized(free) { free.clear() }
    }

    companion object {
        fun rangeFor(lo: Float, hi: Float, span: Float): IntRange = floor(lo / span).toInt()..floor((hi - 0.001f) / span).toInt()
        fun snap(v: Float): Float = v.roundToInt().toFloat()
    }
}
