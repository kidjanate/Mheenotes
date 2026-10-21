package com.mheenotes.core.geometry

import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.newId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object StrokeGeometry {
    /** Radius for one sample. Wet and dry rendering both call this, so they never disagree. */
    fun radius(tool: Tool, width: Float, pressure: Float): Float {
        val p = pressure.coerceIn(0f, 1f)
        val half = width / 2f
        return when (tool) {
            Tool.Pen -> half * (0.28f + 0.72f * p.pow(0.8f))
            Tool.Pencil -> half * (0.55f + 0.45f * p)
            Tool.Highlighter -> half
        }
    }

    fun maxRadius(s: Stroke): Float = when (s.tool) {
        Tool.Highlighter -> s.width / 2f
        else -> {
            var m = 0f
            var i = 2
            while (i < s.points.size) {
                m = max(m, s.points[i]); i += 3
            }
            radius(s.tool, s.width, m)
        }
    }

    fun bounds(s: Stroke): Rect {
        val p = s.points
        if (p.isEmpty()) return Rect.Empty
        var l = p[0]; var t = p[1]; var r = p[0]; var b = p[1]
        var i = 3
        while (i < p.size) {
            val x = p[i]; val y = p[i + 1]
            if (x < l) l = x else if (x > r) r = x
            if (y < t) t = y else if (y > b) b = y
            i += 3
        }
        return Rect(l, t, r, b).outset(maxRadius(s) + 0.5f)
    }

    /** True when a circle at (x, y) with radius [r] touches the stroke's inked area. */
    fun hits(s: Stroke, x: Float, y: Float, r: Float): Boolean {
        if (!s.bounds.outset(r).contains(x, y)) return false
        val p = s.points
        val n = s.pointCount
        if (n == 1) return dist(p[0], p[1], x, y) <= r + radius(s.tool, s.width, p[2])
        for (i in 0 until n - 1) {
            val a = i * 3
            val reach = r + radius(s.tool, s.width, max(p[a + 2], p[a + 5]))
            if (segmentDistance(x, y, p[a], p[a + 1], p[a + 3], p[a + 4]) <= reach) return true
        }
        return false
    }

    /** True when the segment from (x0,y0) to (x1,y1) with radius [r] touches the stroke. */
    fun hitsSegment(s: Stroke, x0: Float, y0: Float, x1: Float, y1: Float, r: Float): Boolean {
        val len = dist(x0, y0, x1, y1)
        val steps = max(1, (len / max(r, 0.5f)).toInt())
        for (k in 0..steps) {
            val t = k.toFloat() / steps
            if (hits(s, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r)) return true
        }
        return false
    }

    /**
     * Cuts the part of [s] inside the circle and returns the surviving pieces, or null when
     * the circle misses. Crossing segments are split at the exact circle boundary.
     */
    fun eraseCircle(s: Stroke, cx: Float, cy: Float, r: Float, nextSeq: () -> Long): List<Stroke>? {
        if (!hits(s, cx, cy, r)) return null
        val p = s.points
        val n = s.pointCount
        val pieces = ArrayList<Stroke>()
        var current = FloatList()
        fun inside(i: Int): Boolean {
            val a = i * 3
            return dist(p[a], p[a + 1], cx, cy) <= r + radius(s.tool, s.width, p[a + 2]) * 0.5f
        }
        fun flush() {
            if (current.size >= 3) pieces += Stroke(newId(), nextSeq(), s.tool, s.argb, s.width, current.toArray())
            current = FloatList()
        }
        for (i in 0 until n) {
            val a = i * 3
            val inNow = inside(i)
            if (i > 0) {
                val b = a - 3
                val inPrev = inside(i - 1)
                if (inPrev != inNow) {
                    val t = circleCrossing(p[b], p[b + 1], p[a], p[a + 1], cx, cy, r)
                    val ix = p[b] + (p[a] - p[b]) * t
                    val iy = p[b + 1] + (p[a + 1] - p[b + 1]) * t
                    val ip = p[b + 2] + (p[a + 2] - p[b + 2]) * t
                    current.add(ix, iy, ip)
                    if (!inPrev) flush()
                }
            }
            if (!inNow) current.add(p[a], p[a + 1], p[a + 2])
        }
        flush()
        // Single-sample remnants are invisible specks; drop them.
        return pieces.filter { it.pointCount >= 2 || n == 1 }
    }

    fun transform(s: Stroke, t: Similarity, seq: Long = s.seq): Stroke {
        val src = s.points
        val out = FloatArray(src.size)
        var i = 0
        while (i < src.size) {
            out[i] = t.mapX(src[i], src[i + 1])
            out[i + 1] = t.mapY(src[i], src[i + 1])
            out[i + 2] = src[i + 2]
            i += 3
        }
        return Stroke(s.id, seq, s.tool, s.argb, s.width * t.scale, out)
    }

    /**
     * Drops samples closer than [minDistance] to the previously kept one. Used when a page is
     * rendered small, where sub-pixel detail costs time but is invisible.
     */
    fun decimate(points: FloatArray, minDistance: Float): FloatArray {
        if (points.size <= 6 || minDistance <= 0f) return points
        val out = FloatList(points.size / 3)
        out.add(points[0], points[1], points[2])
        var lx = points[0]; var ly = points[1]
        val min2 = minDistance * minDistance
        var i = 3
        val last = points.size - 3
        while (i < last) {
            val dx = points[i] - lx; val dy = points[i + 1] - ly
            if (dx * dx + dy * dy >= min2) {
                out.add(points[i], points[i + 1], points[i + 2])
                lx = points[i]; ly = points[i + 1]
            }
            i += 3
        }
        out.add(points[last], points[last + 1], points[last + 2])
        return if (out.size == points.size) points else out.toArray()
    }

    fun dist(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0; val dy = y1 - y0
        return sqrt(dx * dx + dy * dy)
    }

    fun segmentDistance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
        return dist(px, py, ax + dx * t, ay + dy * t)
    }

    /** Parameter t in [0,1] where segment a→b crosses the circle boundary (closest root). */
    private fun circleCrossing(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, r: Float): Float {
        val dx = bx - ax; val dy = by - ay
        val fx = ax - cx; val fy = ay - cy
        val a = dx * dx + dy * dy
        if (a == 0f) return 0f
        val b = 2 * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - r * r
        val disc = b * b - 4 * a * c
        if (disc < 0) return 0.5f
        val sq = sqrt(disc)
        val t1 = (-b - sq) / (2 * a)
        val t2 = (-b + sq) / (2 * a)
        return when {
            t1 in 0f..1f -> t1
            t2 in 0f..1f -> t2
            else -> 0.5f
        }
    }
}

/** Growable float buffer without boxing. */
class FloatList(capacity: Int = 64) {
    private var data = FloatArray(capacity * 3)
    var size = 0
        private set

    fun add(a: Float, b: Float, c: Float) {
        if (size + 3 > data.size) data = data.copyOf(max(data.size * 2, 48))
        data[size] = a; data[size + 1] = b; data[size + 2] = c
        size += 3
    }

    operator fun get(i: Int): Float = data[i]
    fun clear() { size = 0 }
    fun toArray(): FloatArray = data.copyOf(size)
}

/**
 * Causal smoothing applied while samples arrive. Because it only looks backwards, the wet
 * stroke on screen and the stored stroke are identical.
 */
class InkSmoother(private val strength: Float = 0.35f) {
    private var has = false
    private var sx = 0f
    private var sy = 0f
    private var sp = 0f

    fun reset() { has = false }

    fun add(x: Float, y: Float, p: Float, out: FloatList) {
        if (!has) {
            sx = x; sy = y; sp = p; has = true
        } else {
            val k = 1f - strength
            sx += (x - sx) * k
            sy += (y - sy) * k
            sp += (p - sp) * 0.5f
        }
        out.add(sx, sy, sp)
    }
}

fun Item.layer(): Int = if (this is Stroke && tool == Tool.Highlighter) 0 else 1

/** Highlighters sit under everything else; within a layer, z-order wins. */
val ItemDrawOrder: Comparator<Item> = compareBy<Item>({ it.layer() }, { it.seq })
