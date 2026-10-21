package com.mheenotes.ui.editor

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.AngleSnap
import com.mheenotes.core.geometry.FloatList
import com.mheenotes.core.geometry.InkSmoother
import com.mheenotes.core.geometry.Lasso
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.geometry.ScribbleDetector
import com.mheenotes.core.geometry.ShapeRecognizer
import com.mheenotes.core.geometry.Similarity
import com.mheenotes.core.geometry.StrokeGeometry
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.render.drawInkStroke
import com.mheenotes.ui.render.drawShape
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Colors and callbacks the canvas needs from the Compose chrome. */
class CanvasChrome(
    var desk: Int,
    var accent: Int,
    var shadow: Int,
    var topInset: Float,
    var bottomInset: Float,
    val onTextTap: (pageId: Id, x: Float, y: Float, existing: TextItem?) -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
)

/**
 * The page canvas. One View draws cached tiles, then live overlays (wet ink, pending strokes,
 * lasso, selection) in the same frame, so finished strokes never flicker on handoff.
 */
@SuppressLint("ViewConstructor")
class InkCanvasView(
    context: Context,
    val tab: EditorTab,
    val doc: NotebookDocument,
    rasterizer: Rasterizer,
    val chrome: CanvasChrome,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val tiles = TileCache(tab.notebookId, rasterizer, ::postInvalidateOnAnimation)
    private val painter = rasterizer.painter(tab.notebookId)
    private val drawScope = CanvasDrawScope()

    private var layout = PageLayout(doc.pages.value)
    private var scale = 1f
    private var tx = 0f
    private var ty = 0f
    private var fitScale = 1f
    private var initialized = false
    /** Tile level frozen while pinching, so tiles are not rebuilt every frame. */
    private var lockedLevel: Int? = null

    private val loading = HashSet<Id>()

    /** Front-buffered wet ink; null when low-latency ink is off or unsupported. */
    var front: FrontInk? = null
    private var frontDrawn = 0
    private var frontClearPending = false
    private val frontActive: Boolean get() = front != null && wetTool != Tool.Highlighter && snappedShape == null

    init {
        isFocusable = true
        doc.onContent = { pageId, removed, added ->
            val dirty = (removed + added).fold<Item, Rect?>(null) { acc, it -> acc?.union(it.bounds) ?: it.bounds }
            if (dirty != null) tiles.invalidate(pageId, dirty.outset(1f))
            postInvalidateOnAnimation()
        }
    }

    // ---- Pages & viewport ----------------------------------------------------------------

    fun setPages(pages: List<Page>) {
        val anchorPage = currentPageIndex().coerceAtLeast(0)
        val anchorId = layout.pages.getOrNull(anchorPage)?.id
        val anchorOffset = if (anchorId != null) (chrome.topInset - ty) / scale - layout.tops[anchorPage] else 0f
        val old = layout.pages.associateBy { it.id }
        layout = PageLayout(pages)
        pages.forEach { p -> if (old[p.id] != null && old[p.id] != p) tiles.invalidatePage(p.id) }
        tab.viewport.layout = layout
        if (initialized && anchorId != null) {
            val i = layout.indexOf(anchorId)
            if (i >= 0) ty = chrome.topInset - (layout.tops[i] + anchorOffset) * scale
        }
        computeFit()
        clamp()
        publish()
        invalidate()
    }

    private fun computeFit() {
        if (width == 0) return
        val maxContent = min(width - 64 * density, 1000 * density)
        fitScale = maxContent / layout.width
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // A resized window must never show a stale low-latency frame on top.
        front?.clear()
        val zoom = if (initialized) scale / fitScale else tab.zoom
        val topDoc = if (initialized) (chrome.topInset - ty) / scale else tab.scrollY
        computeFit()
        scale = fitScale * zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        ty = chrome.topInset - topDoc * scale
        tx = (w - layout.width * scale) / 2
        initialized = true
        tab.viewport.layout = layout
        clamp()
        publish()
    }

    private fun clamp() {
        if (width == 0) return
        val cw = layout.width * scale
        tx = if (cw <= width) (width - cw) / 2 else tx.coerceIn(width - cw - 24 * density, 24 * density)
        val ch = layout.height * scale
        val minTy = height - chrome.bottomInset - ch
        val maxTy = chrome.topInset
        ty = if (minTy > maxTy) maxTy else ty.coerceIn(minTy, maxTy)
    }

    private fun publish() {
        tab.viewport.scale = scale
        tab.viewport.tx = tx
        tab.viewport.ty = ty
        tab.zoom = scale / fitScale
        tab.scrollY = (chrome.topInset - ty) / scale
        val i = currentPageIndex()
        if (i >= 0 && i != tab.currentPage) tab.currentPage = i
    }

    private fun currentPageIndex(): Int = layout.indexAt((height * 0.4f - ty) / scale)

    fun scrollToPage(index: Int, animate: Boolean) {
        if (index !in layout.pages.indices) return
        val target = chrome.topInset + 12 * density - layout.tops[index] * scale
        animateTo(scale, tx, target, animate)
    }

    fun fitWidth() {
        val docY = (height / 2f - ty) / scale
        val s = fitScale
        animateTo(s, (width - layout.width * s) / 2, height / 2f - docY * s, true)
    }

    private var animator: ValueAnimator? = null

    private fun animateTo(s1: Float, tx1: Float, ty1: Float, animate: Boolean) {
        animator?.cancel()
        scroller.forceFinished(true)
        if (!animate) {
            scale = s1; tx = tx1; ty = ty1; clamp(); publish(); invalidate(); return
        }
        val s0 = scale; val tx0 = tx; val ty0 = ty
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380
            interpolator = DecelerateInterpolator(2.2f)
            addUpdateListener {
                val f = it.animatedValue as Float
                scale = s0 + (s1 - s0) * f; tx = tx0 + (tx1 - tx0) * f; ty = ty0 + (ty1 - ty0) * f
                clamp(); publish(); invalidate()
            }
            start()
        }
    }

    // ---- Drawing --------------------------------------------------------------------------

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()

    override fun onDraw(canvas: Canvas) {
        if (scroller.computeScrollOffset()) {
            tx = scroller.currX.toFloat(); ty = scroller.currY.toFloat()
            clamp(); publish()
            postInvalidateOnAnimation()
        }
        canvas.drawColor(chrome.desk)
        if (layout.pages.isEmpty()) return
        val level = lockedLevel ?: levelFor(scale)
        val vTop = (0 - ty) / scale
        val vBottom = (height - ty) / scale
        val vLeft = (0 - tx) / scale
        val vRight = (width - tx) / scale
        val first = layout.indexAt(vTop)
        val last = layout.indexAt(vBottom)
        val keep = HashSet<Id>()
        val cx = width / 2f; val cy = height / 2f

        shadowPaint.color = 0
        shadowPaint.setShadowLayer(10 * density, 0f, 2 * density, chrome.shadow)

        for (i in first..last) {
            val page = layout.pages[i]
            val pl = layout.left(i); val pt = layout.tops[i]
            val sl = TileCache.snap(pl * scale + tx); val st = TileCache.snap(pt * scale + ty)
            val sr = TileCache.snap((pl + page.width) * scale + tx); val sb = TileCache.snap((pt + page.height) * scale + ty)
            if (sb < 0 || st > height) continue
            keep += page.id
            shadowPaint.color = page.backgroundArgb
            canvas.drawRect(sl, st, sr, sb, shadowPaint)
            pagePaint.color = page.backgroundArgb
            canvas.drawRect(sl, st, sr, sb, pagePaint)

            val content = doc.content(page.id)
            if (content == null) { ensureLoaded(page.id); continue }

            canvas.save()
            canvas.clipRect(sl, st, sr, sb)
            // Whole-page preview underneath: something sharp-enough is always on screen.
            val previewLevel = levelFor(TILE_PX / max(page.width, page.height)) - 1
            if (level > previewLevel) drawLevel(canvas, page, content, i, previewLevel, 0f, 0f, page.width, page.height, cx, cy, 1e6f, true)
            // Current level, limited to the visible part of the page.
            val l = max(0f, vLeft - pl); val r = min(page.width, vRight - pl)
            val t = max(0f, vTop - pt); val b = min(page.height, vBottom - pt)
            if (r > l && b > t) drawLevel(canvas, page, content, i, level, l, t, r, b, cx, cy, 0f, false)
            canvas.restore()

            // Prefetch half a screen beyond the edges at the current level.
            val pre = (vBottom - vTop) * 0.5f
            val pl2 = max(0f, vLeft - pl); val pr2 = min(page.width, vRight - pl)
            val ptop = max(0f, vTop - pt - pre); val pbot = min(page.height, vBottom - pt + pre)
            if (pr2 > pl2 && pbot > ptop) requestLevel(page, content, level, pl2, ptop, pr2, pbot, 2e6f)
        }
        // Make sure neighbours are loaded so scrolling never shows empty pages.
        for (i in max(0, first - 1)..min(layout.pages.lastIndex, last + 1)) {
            keep += layout.pages[i].id
            if (doc.content(layout.pages[i].id) == null) ensureLoaded(layout.pages[i].id)
        }
        if (tiles.frame.get() % 60 == 0L) doc.retainOnly(keep + loading + pendingPages())

        drawOverlays(canvas, level)
        armFrontClear()
        tiles.endFrame()
    }

    private fun drawLevel(
        canvas: Canvas, page: Page, content: com.mheenotes.core.doc.PageContent, index: Int, level: Int,
        l: Float, t: Float, r: Float, b: Float, cx: Float, cy: Float, basePriority: Float, preview: Boolean,
    ) {
        val span = tileSpan(level)
        val pl = layout.left(index); val pt = layout.tops[index]
        val frame = tiles.frame.get()
        for (ty0 in TileCache.rangeFor(t, b, span)) for (tx0 in TileCache.rangeFor(l, r, span)) {
            if (tx0 < 0 || ty0 < 0) continue
            val tile = tiles.getOrCreate(TileKey(page.id, level, tx0, ty0))
            val left = (pl + tx0 * span) * scale + tx
            val top = (pt + ty0 * span) * scale + ty
            val size = span * scale
            tile.bitmap?.let { bmp ->
                dst.set(TileCache.snap(left), TileCache.snap(top), TileCache.snap(left + size), TileCache.snap(top + size))
                canvas.drawBitmap(bmp, null, dst, bitmapPaint)
                tile.lastDrawn = frame
            }
            val d = hypot(left + size / 2 - cx, top + size / 2 - cy)
            tiles.request(tile, page, content, basePriority + d)
            if (preview) tile.lastDrawn = frame
        }
    }

    private fun requestLevel(page: Page, content: com.mheenotes.core.doc.PageContent, level: Int, l: Float, t: Float, r: Float, b: Float, priority: Float) {
        val span = tileSpan(level)
        for (ty0 in TileCache.rangeFor(t, b, span)) for (tx0 in TileCache.rangeFor(l, r, span)) {
            if (tx0 < 0 || ty0 < 0) continue
            val tile = tiles.getOrCreate(TileKey(page.id, level, tx0, ty0))
            if (tile.bitmap == null || !tile.fresh) tiles.request(tile, page, content, priority + ty0)
        }
    }

    private fun ensureLoaded(pageId: Id) {
        if (!loading.add(pageId)) return
        tab.scope.launch {
            runCatching { doc.load(pageId) }
            loading.remove(pageId)
            invalidate()
        }
    }

    /** True once every current-level tile under [r] on [pageId] shows up-to-date content. */
    private fun tilesFresh(pageId: Id, r: Rect, level: Int): Boolean {
        val span = tileSpan(level)
        for (ty0 in TileCache.rangeFor(r.top, r.bottom, span)) for (tx0 in TileCache.rangeFor(r.left, r.right, span)) {
            if (tx0 < 0 || ty0 < 0) continue
            val t = tiles.get(TileKey(pageId, level, tx0, ty0)) ?: continue
            if (!t.fresh) return false
        }
        return true
    }

    // ---- Overlays -------------------------------------------------------------------------

    /** Strokes already saved but not yet visible in tiles. Drawn as vectors until they are. */
    private val pending = ArrayList<Pair<Id, Item>>()

    private fun pendingPages() = pending.mapTo(HashSet()) { it.first }

    private inline fun pageSpace(canvas: Canvas, pageId: Id, crossinline block: DrawScope.() -> Unit) {
        val i = layout.indexOf(pageId)
        if (i < 0) return
        val ox = layout.left(i) * scale + tx
        val oy = layout.tops[i] * scale + ty
        drawScope.draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(width.toFloat(), height.toFloat())) {
            withTransform({ translate(ox, oy); scale(scale, scale, Offset.Zero) }) { block() }
        }
    }

    private val pendingUnloaded = ArrayList<Pair<Id, Stroke>>()

    private fun drawOverlays(canvas: Canvas, level: Int) {
        for ((pageId, s) in pendingUnloaded) pageSpace(canvas, pageId) { drawInkStroke(s) }
        if (pending.isNotEmpty()) {
            pending.removeAll { (pageId, item) -> tilesFresh(pageId, item.bounds, level) || doc.content(pageId)?.get(item.id) == null }
            for ((pageId, item) in pending) pageSpace(canvas, pageId) {
                when (item) {
                    is Stroke -> drawInkStroke(item)
                    is ShapeItem -> drawShape(item)
                    else -> Unit
                }
            }
        }
        val wet = wetStroke
        if (wet != null && wetPage != null && !(frontActive && mode == Mode.Ink)) pageSpace(canvas, wetPage!!) {
            val shape = snappedShape
            if (shape != null) drawShape(shape) else drawInkStroke(wet)
        }
        shapeDraft?.let { s -> pageSpace(canvas, wetPage ?: return@let) { drawShape(s) } }
        lassoPoints?.let { pts -> if (wetPage != null && pts.size >= 6) pageSpace(canvas, wetPage!!) { drawLasso(pts) } }
        tab.captureArea?.let { area ->
            val pts = FloatList(area.lasso.size / 2)
            var k = 0
            while (k < area.lasso.size) { pts.add(area.lasso[k], area.lasso[k + 1], 0f); k += 2 }
            pageSpace(canvas, area.pageId) { drawLasso(pts) }
        }
        tab.selection?.let { sel -> pageSpace(canvas, sel.pageId) { drawSelection(sel) } }
        if (eraserAt != null && wetPage != null) pageSpace(canvas, wetPage!!) {
            val (x, y) = eraserAt!!
            val r = Tools.eraserWidth / 2
            drawCircle(Color(0x22000000), r, Offset(x, y))
            drawCircle(Color(0x66000000), r, Offset(x, y), style = DrawStroke(1f / scale * density))
        }
    }

    private fun DrawScope.drawLasso(pts: FloatList) {
        val path = androidx.compose.ui.graphics.Path()
        // Lasso samples are stored as x, y, 0 triples.
        path.moveTo(pts[0], pts[1])
        var i = 3
        while (i < pts.size) { path.lineTo(pts[i], pts[i + 1]); i += 3 }
        path.close()
        val accent = Color(chrome.accent)
        drawPath(path, accent.copy(alpha = 0.08f))
        drawPath(path, accent, style = DrawStroke(1.6f * density / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density / scale, 5f * density / scale))))
    }

    private fun DrawScope.drawSelection(sel: Selection) {
        val t = sel.transform
        val b = sel.bounds
        withTransform({
            translate(t.dx, t.dy)
            rotate(t.rotation, Offset(t.pivotX, t.pivotY))
            scale(t.scale, t.scale, Offset(t.pivotX, t.pivotY))
        }) {
            with(painter) { draw(sel.items.sortedWith(com.mheenotes.core.geometry.ItemDrawOrder), scale * t.scale) }
        }
        // Frame and handles are drawn at constant screen size.
        val accent = Color(chrome.accent)
        val px = density / scale
        val pad = 8 * px
        val corners = selectionCorners(sel, pad)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(corners[0], corners[1]); lineTo(corners[2], corners[3]); lineTo(corners[4], corners[5]); lineTo(corners[6], corners[7]); close()
        }
        drawPath(path, accent, style = DrawStroke(1.5f * px, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5 * px, 4 * px))))
        val (sx, sy) = scaleHandle(sel, pad)
        drawCircle(Color.White, 9 * px, Offset(sx, sy))
        drawCircle(accent, 9 * px, Offset(sx, sy), style = DrawStroke(2 * px))
        val (rx, ry) = rotateHandle(sel, pad)
        val (tcx, tcy) = topCenter(sel, pad)
        drawLine(accent, Offset(tcx, tcy), Offset(rx, ry), 1.5f * px, StrokeCap.Round)
        drawCircle(accent, 8 * px, Offset(rx, ry))
        drawCircle(Color.White, 3 * px, Offset(rx, ry))
    }

    private fun mapped(sel: Selection, x: Float, y: Float): Pair<Float, Float> {
        val t = sel.transform
        return t.mapX(x, y) to t.mapY(x, y)
    }

    private fun selectionCorners(sel: Selection, pad: Float): FloatArray {
        val b = sel.bounds.outset(pad / sel.transform.scale)
        val (ax, ay) = mapped(sel, b.left, b.top)
        val (bx, by) = mapped(sel, b.right, b.top)
        val (cx, cy) = mapped(sel, b.right, b.bottom)
        val (dx, dy) = mapped(sel, b.left, b.bottom)
        return floatArrayOf(ax, ay, bx, by, cx, cy, dx, dy)
    }

    private fun scaleHandle(sel: Selection, pad: Float) = selectionCorners(sel, pad).let { it[4] to it[5] }

    private fun topCenter(sel: Selection, pad: Float) = selectionCorners(sel, pad).let { (it[0] + it[2]) / 2 to (it[1] + it[3]) / 2 }

    private fun rotateHandle(sel: Selection, pad: Float): Pair<Float, Float> {
        val (cx, cy) = topCenter(sel, pad)
        val ang = (sel.transform.rotation - 90f) * (Math.PI / 180).toFloat()
        val len = 30 * density / scale
        return cx + kotlin.math.cos(ang) * len to cy + kotlin.math.sin(ang) * len
    }

    // ---- Input ----------------------------------------------------------------------------

    private enum class Mode { None, Ink, Erase, Lasso, Shape, SelMove, SelScale, SelRotate, Text, Pan, Pinch, Ignore }

    private var mode = Mode.None
    private var activePointer = -1
    private var stylusNearUntil = 0L

    private var wetPage: Id? = null
    private var wetPoints = FloatList()
    private var wetTool = Tool.Pen
    private var wetColor = 0
    private var wetWidth = 1f
    private var wetStroke: Stroke? = null
    private val smoother = InkSmoother(0.32f)
    private var lastSampleX = 0f
    private var lastSampleY = 0f
    private var lastMoveAt = 0L
    private var snappedShape: ShapeItem? = null

    private var eraserAt: Pair<Float, Float>? = null
    private val erasedOriginals = LinkedHashMap<Id, Item>()
    private val erasedAdded = LinkedHashMap<Id, Item>()

    private var lassoPoints: FloatList? = null
    private var shapeDraft: ShapeItem? = null
    private var shapeStart: Pair<Float, Float>? = null
    private var gestureStart: Pair<Float, Float>? = null
    private var selStart: Similarity? = null

    private val scroller = OverScroller(context)
    private var velocity: VelocityTracker? = null
    private var panLastX = 0f
    private var panLastY = 0f
    private var pinchDist = 0f
    private var pinchMidX = 0f
    private var pinchMidY = 0f
    private var multiTapStart = 0L
    private var multiTapMaxPointers = 0
    private var multiTapMoved = false

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
            stylusNearUntil = SystemClock.uptimeMillis() + 400
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun isStylus(e: MotionEvent, idx: Int) =
        e.getToolType(idx) == MotionEvent.TOOL_TYPE_STYLUS || e.getToolType(idx) == MotionEvent.TOOL_TYPE_ERASER

    /** Page-local coordinates for a screen point, with the page index. */
    private fun toPage(x: Float, y: Float, pageIndex: Int? = null): Triple<Int, Float, Float> {
        val dx = (x - tx) / scale
        val dy = (y - ty) / scale
        val i = pageIndex ?: layout.indexAt(dy)
        return Triple(i, dx - layout.left(i), dy - layout.tops[i])
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> onDown(e)
            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(e)
            MotionEvent.ACTION_MOVE -> onMove(e)
            MotionEvent.ACTION_POINTER_UP -> onPointerUp(e)
            MotionEvent.ACTION_UP -> onUp(e, cancelled = false)
            MotionEvent.ACTION_CANCEL -> onUp(e, cancelled = true)
        }
        return true
    }

    private fun onDown(e: MotionEvent) {
        if (tab.captureArea != null) { tab.captureArea = null; invalidate() }
        if (tab.textEdit != null) {
            tab.finishText?.invoke()
            mode = Mode.Ignore
            return
        }
        animator?.cancel()
        scroller.forceFinished(true)
        activePointer = e.getPointerId(0)
        gestureStart = e.x to e.y
        multiTapStart = e.eventTime; multiTapMaxPointers = 1; multiTapMoved = false
        val stylus = isStylus(e, 0)
        if (!stylus && SystemClock.uptimeMillis() < stylusNearUntil) { mode = Mode.Ignore; return }
        val fingerDraws = !stylus && AppGraph.settings.fingerDraws
        val eraserEnd = e.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER ||
            (stylus && e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0)

        // Selection handles take priority for any pointer.
        tab.selection?.let { sel ->
            val hit = selectionHit(sel, e.x, e.y)
            if (hit != null) { mode = hit; selStart = sel.transform; return }
            // A finger outside the selection scrolls; a pen there puts the selection down.
            if (stylus) commitSelection()
        }
        val drawing = stylus || fingerDraws
        if (!drawing) { startPan(e); return }

        val (pi, x, y) = toPage(e.x, e.y)
        if (pi < 0) { mode = Mode.Ignore; return }
        val page = layout.pages[pi]
        // Ink never waits for page data; it is saved once the page finishes loading.
        if (doc.content(page.id) == null) {
            ensureLoaded(page.id)
            if (!(Tools.isInk && !eraserEnd)) { mode = Mode.Ignore; return }
        }
        wetPage = page.id
        when {
            eraserEnd || Tools.current == ToolKind.Eraser -> { mode = Mode.Erase; erasedOriginals.clear(); erasedAdded.clear(); lastSampleX = x; lastSampleY = y; eraseAt(x, y, x, y) }
            Tools.isInk -> startInk(e, x, y)
            Tools.current == ToolKind.Lasso -> { mode = Mode.Lasso; lassoPoints = FloatList().also { it.add(x, y, 0f) } }
            Tools.current == ToolKind.Shape -> { mode = Mode.Shape; shapeStart = x to y }
            Tools.current == ToolKind.Text -> mode = Mode.Text
            else -> startPan(e)
        }
        invalidate()
    }

    private fun startInk(e: MotionEvent, x: Float, y: Float) {
        mode = Mode.Ink
        tab.writing = true
        wetTool = Tools.inkTool() ?: Tool.Pen
        wetColor = Tools.colorOf(Tools.current)
        wetWidth = Tools.widthOf(Tools.current)
        wetPoints = FloatList(128)
        smoother.reset()
        snappedShape = null
        addSample(x, y, pressureOf(e, 0, -1), force = true)
        holdX = x; holdY = y
        if (frontClearPending) { front?.clear(); frontClearPending = false }
        frontDrawn = 0
        sendFront()
        lastMoveAt = SystemClock.uptimeMillis()
        scheduleHoldCheck()
    }

    private fun pressureOf(e: MotionEvent, idx: Int, h: Int): Float {
        // Without pressure the pen draws at a steady, slightly-below-full width.
        if (!isStylus(e, idx) || !AppGraph.settings.pressure) return 0.55f
        val p = if (h < 0) e.getPressure(idx) else e.getHistoricalPressure(idx, h)
        return p.coerceIn(0.02f, 1f)
    }

    private fun addSample(x: Float, y: Float, p: Float, force: Boolean = false) {
        if (!force && hypot(x - lastSampleX, y - lastSampleY) * scale < 0.6f * density) return
        lastSampleX = x; lastSampleY = y
        smoother.add(x, y, p, wetPoints)
        wetStroke = Stroke("wet", 0, wetTool, wetColor, wetWidth, wetPoints.toArray())
    }

    private val holdCheck = Runnable {
        if (mode != Mode.Ink || snappedShape != null || !AppGraph.settings.holdToShape || wetTool == Tool.Highlighter) return@Runnable
        if (SystemClock.uptimeMillis() - lastMoveAt < HOLD_MS - 20) { scheduleHoldCheck(); return@Runnable }
        val pts = wetPoints.toArray()
        // A scribble that pauses at the end is an erase gesture, not a shape.
        if (ScribbleDetector.isScribble(pts)) return@Runnable
        val rec = ShapeRecognizer.recognize(pts) ?: return@Runnable
        snappedShape = ShapeItem(newId(), 0, rec.kind, rec.frame, wetColor, max(wetWidth, 1f))
        front?.clear()
        performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        invalidate()
    }

    private fun scheduleHoldCheck() {
        removeCallbacks(holdCheck)
        postDelayed(holdCheck, HOLD_MS)
    }

    private fun onPointerDown(e: MotionEvent) {
        multiTapMaxPointers = max(multiTapMaxPointers, e.pointerCount)
        when (mode) {
            Mode.Pan, Mode.None -> startPinch(e)
            Mode.Ink -> if (!isStylus(e, 0)) { cancelWet(); startPinch(e) }
            Mode.Ignore -> if (!isStylus(e, e.actionIndex) && SystemClock.uptimeMillis() >= stylusNearUntil) startPinch(e)
            else -> Unit
        }
    }

    private fun onPointerUp(e: MotionEvent) {
        if (mode == Mode.Pinch && e.pointerCount - 1 < 2) {
            lockedLevel = null
            val remaining = if (e.actionIndex == 0) 1 else 0
            panLastX = e.getX(remaining); panLastY = e.getY(remaining)
            activePointer = e.getPointerId(remaining)
            velocity?.clear()
            mode = Mode.Pan
            invalidate()
        }
    }

    private fun onMove(e: MotionEvent) {
        gestureStart?.let { (sx, sy) -> if (hypot(e.x - sx, e.y - sy) > 12 * density) multiTapMoved = true }
        when (mode) {
            Mode.Ink -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                if (snappedShape != null) return
                val pi = layout.indexOf(wetPage ?: return)
                for (h in 0 until e.historySize) {
                    val (_, x, y) = toPage(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), pi)
                    addSample(x, y, pressureOf(e, idx, h))
                }
                val (_, x, y) = toPage(e.getX(idx), e.getY(idx), pi)
                val before = wetPoints.size
                addSample(x, y, pressureOf(e, idx, -1))
                sendFront()
                if (wetPoints.size != before && hypot(x - holdX, y - holdY) * scale > 2 * density) {
                    holdX = x; holdY = y
                    lastMoveAt = SystemClock.uptimeMillis()
                    scheduleHoldCheck()
                }
                invalidate()
            }
            Mode.Erase -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                val pi = layout.indexOf(wetPage ?: return)
                for (h in 0..e.historySize) {
                    val sx = if (h < e.historySize) e.getHistoricalX(idx, h) else e.getX(idx)
                    val sy = if (h < e.historySize) e.getHistoricalY(idx, h) else e.getY(idx)
                    val (_, x, y) = toPage(sx, sy, pi)
                    eraseAt(lastSampleX, lastSampleY, x, y)
                    lastSampleX = x; lastSampleY = y
                }
                invalidate()
            }
            Mode.Lasso -> {
                val pi = layout.indexOf(wetPage ?: return)
                val (_, x, y) = toPage(e.x, e.y, pi)
                lassoPoints?.let { pts ->
                    val n = pts.size
                    if (n < 3 || hypot(x - pts[n - 3], y - pts[n - 2]) * scale > 2 * density) pts.add(x, y, 0f)
                }
                invalidate()
            }
            Mode.Shape -> {
                val pi = layout.indexOf(wetPage ?: return)
                val (_, x, y) = toPage(e.x, e.y, pi)
                val (sx, sy) = shapeStart ?: return
                // Lines and arrows lock to 45° steps when drawn close to one.
                val (ex, ey) = if (Tools.shapeKind == ShapeKind.Line || Tools.shapeKind == ShapeKind.Arrow) AngleSnap.snapEnd(sx, sy, x, y) else x to y
                shapeDraft = ShapeItem(
                    "draft", 0, Tools.shapeKind, Rect(sx, sy, ex, ey), Tools.shapeColor, Tools.shapeWidth,
                    if (Tools.shapeFill) (Tools.shapeColor and 0x00FFFFFF) or 0x33000000 else null,
                )
                invalidate()
            }
            Mode.SelMove, Mode.SelScale, Mode.SelRotate -> moveSelection(e)
            Mode.Pan -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                velocity?.addMovement(e)
                tx += e.getX(idx) - panLastX; ty += e.getY(idx) - panLastY
                panLastX = e.getX(idx); panLastY = e.getY(idx)
                clamp(); publish(); invalidate()
            }
            Mode.Pinch -> pinch(e)
            else -> Unit
        }
    }

    private var holdX = 0f
    private var holdY = 0f

    private fun onUp(e: MotionEvent, cancelled: Boolean) {
        removeCallbacks(holdCheck)
        val tapped = !multiTapMoved && e.eventTime - multiTapStart < 260
        when (mode) {
            Mode.Ink -> if (cancelled) cancelWet() else finishInk()
            Mode.Erase -> finishErase()
            Mode.Lasso -> if (tapped && !cancelled) { lassoPoints = null; tapSelect(e, includeInk = true) } else finishLasso()
            Mode.Shape -> finishShape()
            Mode.Text -> if (!cancelled) textTap(e)
            Mode.SelMove, Mode.SelScale, Mode.SelRotate -> Unit
            Mode.Pan -> {
                velocity?.let { v ->
                    v.addMovement(e)
                    v.computeCurrentVelocity(1000)
                    val vx = v.xVelocity; val vy = v.yVelocity
                    if (hypot(vx, vy) > 400) {
                        val cw = layout.width * scale
                        val minX = if (cw <= width) tx.toInt() else (width - cw - 24 * density).toInt()
                        val maxX = if (cw <= width) tx.toInt() else (24 * density).toInt()
                        scroller.fling(tx.toInt(), ty.toInt(), vx.toInt(), vy.toInt(), minX, maxX,
                            (height - chrome.bottomInset - layout.height * scale).toInt().coerceAtMost(chrome.topInset.toInt()), chrome.topInset.toInt())
                        postInvalidateOnAnimation()
                    }
                }
                if (tapped && multiTapMaxPointers == 1) {
                    when {
                        tab.selection != null -> commitSelection()
                        Tools.current == ToolKind.Text -> textTap(e)
                        else -> tapSelect(e, includeInk = false)
                    }
                }
            }
            Mode.Pinch -> Unit
            else -> Unit
        }
        if (tapped && !cancelled && multiTapMaxPointers >= 2 && (mode == Mode.Pinch || mode == Mode.Pan)) {
            if (multiTapMaxPointers == 2) chrome.onUndo() else chrome.onRedo()
        }
        lockedLevel = null
        velocity?.recycle(); velocity = null
        if (tab.writing) tab.writing = false
        mode = Mode.None
        eraserAt = null
        lassoPoints = null
        shapeDraft = null
        invalidate()
    }

    private fun startPan(e: MotionEvent) {
        mode = Mode.Pan
        panLastX = e.x; panLastY = e.y
        velocity = VelocityTracker.obtain().also { it.addMovement(e) }
    }

    private fun startPinch(e: MotionEvent) {
        if (e.pointerCount < 2) return
        mode = Mode.Pinch
        lockedLevel = levelFor(scale)
        pinchDist = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
        pinchMidX = (e.getX(0) + e.getX(1)) / 2; pinchMidY = (e.getY(0) + e.getY(1)) / 2
    }

    private fun pinch(e: MotionEvent) {
        if (e.pointerCount < 2) return
        val d = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
        val mx = (e.getX(0) + e.getX(1)) / 2; val my = (e.getY(0) + e.getY(1)) / 2
        if (pinchDist > 0) {
            val newScale = (scale * d / pinchDist).coerceIn(fitScale * MIN_ZOOM, fitScale * MAX_ZOOM)
            val k = newScale / scale
            tx = mx - (pinchMidX - tx) * k
            ty = my - (pinchMidY - ty) * k
            scale = newScale
        }
        if (abs(d - pinchDist) > 8 * density || hypot(mx - pinchMidX, my - pinchMidY) > 8 * density) multiTapMoved = true
        pinchDist = d; pinchMidX = mx; pinchMidY = my
        // Re-bucket tiles once the zoom has moved far from the frozen level.
        lockedLevel?.let { if (abs(levelFor(scale) - it) >= 2) lockedLevel = levelFor(scale) }
        clamp(); publish(); invalidate()
    }

    /** Sends samples not yet on the front buffer, overlapping one so the pieces join. */
    private fun sendFront() {
        val f = front ?: return
        if (!frontActive) return
        val n = wetPoints.size / 3
        if (n <= frontDrawn) return
        val i = layout.indexOf(wetPage ?: return)
        val start = max(0, frontDrawn - 1)
        val pts = FloatArray((n - start) * 3) { wetPoints[start * 3 + it] }
        f.render(WetSegment(wetTool, wetColor, wetWidth, pts, frontDrawn == 0, scale, layout.left(i) * scale + tx, layout.tops[i] * scale + ty))
        frontDrawn = n
    }

    /**
     * Clears the front buffer only once the canvas frame that draws the finished stroke has been
     * handed to the compositor, plus a frame of margin. Clearing earlier makes the stroke blink
     * out for a frame when the pen lifts.
     */
    private fun scheduleFrontClear() {
        if (front == null) return
        frontClearPending = true
        frontClearArmed = false
        invalidate()
    }

    private var frontClearArmed = false

    /** Called at the end of onDraw: the frame being recorded now contains the pending stroke. */
    private fun armFrontClear() {
        if (!frontClearPending || frontClearArmed || mode == Mode.Ink) return
        frontClearArmed = true
        viewTreeObserver.registerFrameCommitCallback {
            postDelayed({
                if (frontClearPending && mode != Mode.Ink) { front?.clear(); frontClearPending = false }
                frontClearArmed = false
            }, 34)
        }
    }

    private fun cancelWet() {
        removeCallbacks(holdCheck)
        front?.clear()
        wetStroke = null; snappedShape = null
        mode = Mode.None
        invalidate()
    }

    private fun finishInk() {
        scheduleFrontClear()
        val pageId = wetPage ?: return
        val pts = wetPoints.toArray()
        wetStroke = null
        val shape = snappedShape
        snappedShape = null
        if (pts.isEmpty()) return
        if (shape != null) {
            val item = shape.copy(seq = doc.nextSeq())
            doc.edit(pageId, emptyList(), listOf(item))
            pending += pageId to item
            return
        }
        if (AppGraph.settings.scribbleErase && wetTool != Tool.Highlighter && ScribbleDetector.isScribble(pts)) {
            val content = doc.content(pageId)
            val near = content?.snapshot?.query(Rect(pts.minX(), pts.minY(), pts.maxX(), pts.maxY()).outset(wetWidth))
                ?.filterIsInstance<Stroke>().orEmpty()
            val victims = ScribbleDetector.victims(pts, wetWidth, near)
            if (victims.isNotEmpty()) {
                doc.edit(pageId, victims, emptyList())
                performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                return
            }
        }
        val stroke = Stroke(newId(), doc.nextSeq(), wetTool, wetColor, wetWidth, pts)
        if (doc.content(pageId) == null) {
            // Keep it on screen until the page loads, then record it.
            pendingUnloaded += pageId to stroke
            tab.scope.launch {
                doc.load(pageId)
                pendingUnloaded.remove(pageId to stroke)
                doc.edit(pageId, emptyList(), listOf(stroke))
                pending += pageId to stroke
                invalidate()
            }
            return
        }
        doc.edit(pageId, emptyList(), listOf(stroke))
        pending += pageId to stroke
    }

    private fun eraseAt(x0: Float, y0: Float, x1: Float, y1: Float) {
        val pageId = wetPage ?: return
        val content = doc.content(pageId) ?: return
        val r = Tools.eraserWidth / 2
        eraserAt = x1 to y1
        val area = Rect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1)).outset(r)
        val hits = content.snapshot.query(area)
        val removed = ArrayList<Item>()
        val added = ArrayList<Item>()
        for (item in hits) {
            when (item) {
                is Stroke -> {
                    if (!StrokeGeometry.hitsSegment(item, x0, y0, x1, y1, r)) continue
                    if (Tools.eraserMode == EraserMode.Whole) {
                        removed += item
                    } else {
                        var pieces: List<Stroke> = listOf(item)
                        val steps = max(1, (hypot(x1 - x0, y1 - y0) / (r * 0.5f)).toInt())
                        for (k in 0..steps) {
                            val t = k.toFloat() / steps
                            val cx = x0 + (x1 - x0) * t; val cy = y0 + (y1 - y0) * t
                            pieces = pieces.flatMap { s -> StrokeGeometry.eraseCircle(s, cx, cy, r) { item.seq } ?: listOf(s) }
                        }
                        removed += item
                        added += pieces
                    }
                }
                is ShapeItem -> if (shapeHit(item, x1, y1, r)) removed += item
                else -> Unit
            }
        }
        if (removed.isEmpty()) return
        for (it in removed) if (erasedAdded.remove(it.id) == null) erasedOriginals[it.id] = it
        for (it in added) erasedAdded[it.id] = it
        doc.preview(pageId, removed, added)
    }

    private fun shapeHit(s: ShapeItem, x: Float, y: Float, r: Float): Boolean {
        val f = s.frame
        return when (s.kind) {
            ShapeKind.Line, ShapeKind.Arrow -> StrokeGeometry.segmentDistance(x, y, f.left, f.top, f.right, f.bottom) <= r + s.strokeWidth
            else -> {
                val n = f.normalized()
                n.outset(r).contains(x, y) && !n.outset(-r - s.strokeWidth).contains(x, y) || (s.fillArgb != null && n.contains(x, y))
            }
        }
    }

    private fun finishErase() {
        val pageId = wetPage ?: return
        if (erasedOriginals.isEmpty() && erasedAdded.isEmpty()) return
        doc.commit(pageId, erasedOriginals.values.toList(), erasedAdded.values.toList())
        erasedOriginals.clear(); erasedAdded.clear()
    }

    private fun finishLasso() {
        val pageId = wetPage ?: return
        val pts = lassoPoints ?: return
        lassoPoints = null
        val xy = FloatArray(pts.size / 3 * 2) { i -> pts[(i / 2) * 3 + (i % 2)] }
        val lasso = Lasso(xy)
        if (!lasso.isUsable) return
        val content = doc.content(pageId) ?: return
        // Locked images stay put; tap one to unlock it.
        val items = content.snapshot.query(lasso.bounds).filter { lasso.selects(it) && !(it is com.mheenotes.core.model.ImageItem && it.locked) }
        // Nothing to move: keep the area so it can still be captured as an image.
        if (items.isEmpty()) { tab.captureArea = CaptureArea(pageId, boundingRect(xy)); return }
        select(pageId, items, xy)
    }

    /** Lifts [items] off the page into an editable selection. */
    fun select(pageId: Id, items: List<Item>, lasso: FloatArray? = null) {
        commitSelection()
        val bounds = items.map { it.bounds }.reduce { a, b -> a.union(b) }
        doc.preview(pageId, items, emptyList())
        tab.selection = Selection(pageId, items, bounds, lasso)
        invalidate()
    }

    /** Drops the selection back onto the page, recording the transform as one undo step. */
    fun commitSelection() {
        val sel = tab.selection ?: return
        tab.selection = null
        val t = sel.transform
        if (t.isIdentity) {
            doc.preview(sel.pageId, emptyList(), sel.items)
        } else {
            val moved = sel.items.map { transformItem(it, t) }
            doc.preview(sel.pageId, emptyList(), moved)
            doc.commit(sel.pageId, sel.items, moved)
            moved.filter { it is Stroke || it is ShapeItem }.forEach { pending += sel.pageId to it }
        }
        invalidate()
    }

    /** Moves the selection [delta] pages away at the same spot (clamped to the new page). */
    fun moveSelectionToPage(delta: Int) {
        val sel = tab.selection ?: return
        val from = layout.indexOf(sel.pageId)
        val target = layout.pages.getOrNull(from + delta) ?: return
        val moved0 = sel.items.map { transformItem(it, sel.transform) }
        val b = moved0.map { it.bounds }.reduce { a, c -> a.union(c) }
        val dx = (-b.left).coerceAtLeast(0f) - (b.right - target.width).coerceAtLeast(0f)
        val dy = (-b.top).coerceAtLeast(0f) - (b.bottom - target.height).coerceAtLeast(0f)
        val moved = moved0.map { transformItem(it, Similarity(0f, 0f, dx = dx, dy = dy)) }
        tab.selection = null
        tab.scope.launch {
            doc.load(target.id)
            // One undo step: gone from the old page, present on the new one.
            doc.batch(listOf(com.mheenotes.core.doc.Change.Items(sel.pageId, sel.items, emptyList()), com.mheenotes.core.doc.Change.Items(target.id, emptyList(), moved)))
            scrollToPage(from + delta, true)
            select(target.id, moved)
        }
    }

    /** Removes the selection's items from the page for good. */
    fun deleteSelection() {
        val sel = tab.selection ?: return
        tab.selection = null
        doc.commit(sel.pageId, sel.items, emptyList())
        invalidate()
    }

    /** Replaces the selection's items (e.g. recolor) keeping them selected. */
    fun replaceSelection(transformer: (Item) -> Item) {
        val sel = tab.selection ?: return
        val t = sel.transform
        val updated = sel.items.map { transformItem(it, t) }.map(transformer)
        doc.preview(sel.pageId, emptyList(), updated)
        doc.commit(sel.pageId, sel.items, updated)
        tab.selection = null
        val lasso = sel.lasso?.let { src -> FloatArray(src.size) { i -> val x = src[i - i % 2]; val y = src[i - i % 2 + 1]; if (i % 2 == 0) t.mapX(x, y) else t.mapY(x, y) } }
        select(sel.pageId, updated, lasso)
    }

    fun currentSelectionItems(): List<Item> {
        val sel = tab.selection ?: return emptyList()
        return sel.items.map { transformItem(it, sel.transform) }
    }

    private fun selectionHit(sel: Selection, sx: Float, sy: Float): Mode? {
        val i = layout.indexOf(sel.pageId)
        if (i < 0) return null
        if (sel.items.any { it is com.mheenotes.core.model.ImageItem && it.locked }) return null
        val (_, x, y) = toPage(sx, sy, i)
        val pad = 8 * density / scale
        val tol = 22 * density / scale
        val (hx, hy) = scaleHandle(sel, pad)
        if (hypot(x - hx, y - hy) < tol) return Mode.SelScale
        val (rx, ry) = rotateHandle(sel, pad)
        if (hypot(x - rx, y - ry) < tol) return Mode.SelRotate
        // Inside the (rotated) frame: invert the transform and test against original bounds.
        val t = sel.transform
        val inv = Similarity(t.pivotX + t.dx, t.pivotY + t.dy, 1f / t.scale, -t.rotation, -t.dx, -t.dy)
        val ox = inv.mapX(x, y); val oy = inv.mapY(x, y)
        return if (sel.bounds.outset(pad).contains(ox, oy)) Mode.SelMove else null
    }

    private fun moveSelection(e: MotionEvent) {
        val sel = tab.selection ?: return
        val start = selStart ?: return
        val (gx, gy) = gestureStart ?: return
        val i = layout.indexOf(sel.pageId)
        val (_, x0, y0) = toPage(gx, gy, i)
        val (_, x, y) = toPage(e.x, e.y, i)
        val cx = start.pivotX + start.dx; val cy = start.pivotY + start.dy
        sel.transform = when (mode) {
            Mode.SelMove -> start.copy(dx = start.dx + x - x0, dy = start.dy + y - y0)
            Mode.SelScale -> {
                val d0 = hypot(x0 - cx, y0 - cy); val d1 = hypot(x - cx, y - cy)
                start.copy(scale = (start.scale * d1 / max(d0, 1f)).coerceIn(0.1f, 20f))
            }
            Mode.SelRotate -> {
                val a0 = atan2(y0 - cy, x0 - cx); val a1 = atan2(y - cy, x - cx)
                val rot = start.rotation + Math.toDegrees((a1 - a0).toDouble()).toFloat()
                start.copy(rotation = AngleSnap.snapRotation(rot))
            }
            else -> start
        }
        invalidate()
    }

    private fun finishShape() {
        val pageId = wetPage ?: return
        val draft = shapeDraft ?: return
        shapeDraft = null
        val f = draft.frame
        if (abs(f.width) * scale < 6 * density && abs(f.height) * scale < 6 * density) return
        val item = draft.copy(id = newId(), seq = doc.nextSeq())
        doc.edit(pageId, emptyList(), listOf(item))
        pending += pageId to item
    }

    /**
     * Tapping an image, text box or shape selects it, so it can be moved or resized without
     * lassoing. With the lasso tool, a tap also picks a single stroke.
     */
    private fun tapSelect(e: MotionEvent, includeInk: Boolean) {
        val (pi, x, y) = toPage(e.x, e.y)
        if (pi < 0) return
        val pageId = layout.pages[pi].id
        val r = 10 * density / scale
        val hit = doc.content(pageId)?.snapshot?.query(Rect.around(x, y, r))?.lastOrNull { item ->
            when (item) {
                is Stroke -> includeInk && StrokeGeometry.hits(item, x, y, r)
                is ShapeItem -> shapeHit(item, x, y, r)
                else -> item.bounds.contains(x, y)
            }
        } ?: return
        if (Tools.current != ToolKind.Lasso) Tools.select(ToolKind.Lasso)
        select(pageId, listOf(hit))
    }

    private fun textTap(e: MotionEvent) {
        val (pi, x, y) = toPage(e.x, e.y)
        if (pi < 0 || x < 0 || y < 0 || x > layout.pages[pi].width || y > layout.pages[pi].height) return
        val pageId = layout.pages[pi].id
        val hit = doc.content(pageId)?.snapshot?.query(Rect.around(x, y, 2f))?.lastOrNull { it is TextItem && it.bounds.contains(x, y) } as? TextItem
        chrome.onTextTap(pageId, x, y, hit)
    }

    fun release() {
        front?.release()
        front = null
        doc.onContent = { _, _, _ -> }
        tiles.close()
        animator?.cancel()
    }

    companion object {
        const val MIN_ZOOM = 0.35f
        const val MAX_ZOOM = 8f
        const val HOLD_MS = 800L

    }
}

private fun FloatArray.minX(): Float { var m = Float.MAX_VALUE; var i = 0; while (i < size) { m = min(m, this[i]); i += 3 }; return m }
private fun FloatArray.maxX(): Float { var m = -Float.MAX_VALUE; var i = 0; while (i < size) { m = max(m, this[i]); i += 3 }; return m }
private fun FloatArray.minY(): Float { var m = Float.MAX_VALUE; var i = 1; while (i < size) { m = min(m, this[i]); i += 3 }; return m }
private fun FloatArray.maxY(): Float { var m = -Float.MAX_VALUE; var i = 1; while (i < size) { m = max(m, this[i]); i += 3 }; return m }
