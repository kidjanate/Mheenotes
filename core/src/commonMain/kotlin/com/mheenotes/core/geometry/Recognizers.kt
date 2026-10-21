package com.mheenotes.core.geometry

import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

data class RecognizedShape(val kind: ShapeKind, val frame: Rect)

/** Turns a held hand-drawn stroke into a clean line, triangle, rectangle or ellipse. */
object ShapeRecognizer {
    fun recognize(points: FloatArray): RecognizedShape? {
        val n = points.size / 3
        if (n < 4) return null
        val xs = FloatArray(n) { points[it * 3] }
        val ys = FloatArray(n) { points[it * 3 + 1] }
        var length = 0f
        for (i in 1 until n) length += StrokeGeometry.dist(xs[i - 1], ys[i - 1], xs[i], ys[i])
        if (length < 24f) return null
        val bounds = Rect(xs.min(), ys.min(), xs.max(), ys.max())
        val diag = StrokeGeometry.dist(bounds.left, bounds.top, bounds.right, bounds.bottom)
        val gap = StrokeGeometry.dist(xs[0], ys[0], xs[n - 1], ys[n - 1])

        // Open stroke: a line if every sample stays close to the chord.
        if (gap > length * 0.22f) {
            var maxDev = 0f
            for (i in 0 until n) maxDev = max(maxDev, StrokeGeometry.segmentDistance(xs[i], ys[i], xs[0], ys[0], xs[n - 1], ys[n - 1]))
            if (maxDev >= max(4f, gap * 0.09f)) return null
            val (ex, ey) = AngleSnap.snapEnd(xs[0], ys[0], xs[n - 1], ys[n - 1])
            return RecognizedShape(ShapeKind.Line, Rect(xs[0], ys[0], ex, ey))
        }

        // Closed stroke: count corners on a simplified outline.
        val keep = douglasPeucker(xs, ys, diag * 0.075f)
        var corners = keep.size - 1 // first and last coincide on a closed loop
        if (corners >= 2) {
            // Merge near-duplicate endpoints that DP keeps on closed loops.
            val first = keep.first(); val last = keep.last()
            if (StrokeGeometry.dist(xs[first], ys[first], xs[last], ys[last]) > diag * 0.25f) corners++
        }
        return when (corners) {
            3 -> RecognizedShape(ShapeKind.Triangle, bounds)
            4 -> RecognizedShape(ShapeKind.Rectangle, bounds)
            // Only clearly round loops become ellipses; wobbly or jagged ones stay ink.
            else -> if (roundness(xs, ys, bounds) < 0.16f) RecognizedShape(ShapeKind.Ellipse, bounds) else null
        }
    }

    /** Mean relative deviation of samples from the bounding ellipse. */
    private fun roundness(xs: FloatArray, ys: FloatArray, b: Rect): Float {
        val rx = b.width / 2; val ry = b.height / 2
        if (rx < 1 || ry < 1) return 1f
        var err = 0f
        for (i in xs.indices) {
            val dx = (xs[i] - b.centerX) / rx; val dy = (ys[i] - b.centerY) / ry
            err += abs(sqrt(dx * dx + dy * dy) - 1f)
        }
        return err / xs.size
    }

    private fun douglasPeucker(xs: FloatArray, ys: FloatArray, eps: Float): List<Int> {
        val keep = BooleanArray(xs.size)
        keep[0] = true; keep[xs.size - 1] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to xs.size - 1)
        while (stack.isNotEmpty()) {
            val (a, b) = stack.removeLast()
            var maxD = 0f; var idx = -1
            for (i in a + 1 until b) {
                val d = StrokeGeometry.segmentDistance(xs[i], ys[i], xs[a], ys[a], xs[b], ys[b])
                if (d > maxD) { maxD = d; idx = i }
            }
            if (idx >= 0 && maxD > eps) {
                keep[idx] = true
                stack.addLast(a to idx); stack.addLast(idx to b)
            }
        }
        // Drop kept points with nearly straight angles; DP can keep them on wobbly edges.
        val raw = keep.indices.filter { keep[it] }
        return raw.filterIndexed { i, p ->
            if (i == 0 || i == raw.size - 1) return@filterIndexed true
            val a = raw[i - 1]; val b = raw[i + 1]
            angle(xs[a] - xs[p], ys[a] - ys[p], xs[b] - xs[p], ys[b] - ys[p]) < 150f
        }
    }

    private fun angle(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val la = sqrt(ax * ax + ay * ay); val lb = sqrt(bx * bx + by * by)
        if (la == 0f || lb == 0f) return 180f
        val cos = ((ax * bx + ay * by) / (la * lb)).coerceIn(-1f, 1f)
        return acos(cos) * 180f / kotlin.math.PI.toFloat()
    }
}

/** Detects a back-and-forth scribble used to erase what it covers. */
object ScribbleDetector {
    fun isScribble(points: FloatArray): Boolean {
        val n = points.size / 3
        if (n < 12) return false
        var length = 0f
        var reversals = 0
        var px = points[3] - points[0]; var py = points[4] - points[1]
        var l = 0f; var t = 0f; var r = 0f; var b = 0f
        l = points[0]; r = l; t = points[1]; b = t
        for (i in 1 until n) {
            val x = points[i * 3]; val y = points[i * 3 + 1]
            val dx = x - points[(i - 1) * 3]; val dy = y - points[(i - 1) * 3 + 1]
            val d = sqrt(dx * dx + dy * dy)
            length += d
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
            if (d > 0.8f) {
                if (px * dx + py * dy < -0.5f * sqrt(px * px + py * py) * d) reversals++
                px = dx; py = dy
            }
        }
        val diag = sqrt((r - l) * (r - l) + (b - t) * (b - t))
        return reversals >= 4 && diag > 6f && length > diag * 3.2f
    }

    /** Strokes the scribble covers: hit by its path and mostly inside its bounds. */
    fun victims(scribble: FloatArray, scribbleWidth: Float, candidates: List<Stroke>): List<Stroke> {
        val n = scribble.size / 3
        var l = scribble[0]; var r = l; var t = scribble[1]; var b = t
        for (i in 1 until n) {
            val x = scribble[i * 3]; val y = scribble[i * 3 + 1]
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
        }
        val area = Rect(l, t, r, b).outset(scribbleWidth * 2)
        return candidates.filter { s ->
            val sb = s.bounds
            if (!sb.intersects(area)) return@filter false
            val ix = minOf(sb.right, area.right) - maxOf(sb.left, area.left)
            val iy = minOf(sb.bottom, area.bottom) - maxOf(sb.top, area.top)
            val covered = (ix * iy) / max(1f, sb.width * sb.height)
            if (covered < 0.4f) return@filter false
            (0 until n - 1).any { i ->
                StrokeGeometry.hitsSegment(s, scribble[i * 3], scribble[i * 3 + 1], scribble[i * 3 + 3], scribble[i * 3 + 4], scribbleWidth / 2)
            }
        }
    }
}

/** Pulls nearly straight lines onto the nearest 45° direction (0°, 45°, 90°, 135°, …). */
object AngleSnap {
    const val STEP = 45f
    const val TOLERANCE = 7f

    /** End point moved so start→end lies on the nearest multiple of [step] when within [tolerance]. */
    fun snapEnd(x0: Float, y0: Float, x1: Float, y1: Float, step: Float = STEP, tolerance: Float = TOLERANCE): Pair<Float, Float> {
        val dx = x1 - x0; val dy = y1 - y0
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0f) return x1 to y1
        val deg = kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
        val target = kotlin.math.round(deg / step) * step
        if (abs(deg - target) > tolerance) return x1 to y1
        val rad = target * kotlin.math.PI.toFloat() / 180f
        return x0 + kotlin.math.cos(rad) * len to y0 + kotlin.math.sin(rad) * len
    }

    /** Rotation in degrees snapped to the nearest multiple of [step] when within [tolerance]. */
    fun snapRotation(degrees: Float, step: Float = STEP, tolerance: Float = 4f): Float {
        val target = kotlin.math.round(degrees / step) * step
        return if (abs(degrees - target) <= tolerance) target else degrees
    }
}
