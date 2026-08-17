package com.mheenotes.ink.engine

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class StrokeSpatialIndex(private val cellSize: Float = 12_000f) {
    private val strokes = mutableMapOf<StrokeId, InkStroke>()
    private val cells = mutableMapOf<Long, MutableSet<StrokeId>>()

    init {
        require(cellSize.isFinite() && cellSize > 0f)
    }

    fun rebuild(source: Iterable<InkStroke>) {
        clear()
        source.forEach(::insert)
    }

    fun insert(stroke: InkStroke) {
        remove(stroke.id)
        strokes[stroke.id] = stroke
        keysFor(stroke.bounds).forEach { key -> cells.getOrPut(key, ::mutableSetOf).add(stroke.id) }
    }

    fun remove(id: StrokeId) {
        val removed = strokes.remove(id) ?: return
        keysFor(removed.bounds).forEach { key ->
            cells[key]?.let { bucket ->
                bucket.remove(id)
                if (bucket.isEmpty()) cells.remove(key)
            }
        }
    }

    fun query(bounds: PageRect): List<InkStroke> = keysFor(bounds)
        .flatMapTo(linkedSetOf()) { cells[it].orEmpty() }
        .mapNotNull(strokes::get)
        .filter { it.bounds.intersects(bounds) }

    fun clear() {
        strokes.clear()
        cells.clear()
    }

    private fun keysFor(bounds: PageRect): Sequence<Long> = sequence {
        val minX = floor(bounds.left / cellSize).toInt()
        val maxX = floor(bounds.right / cellSize).toInt()
        val minY = floor(bounds.top / cellSize).toInt()
        val maxY = floor(bounds.bottom / cellSize).toInt()
        for (y in minY..maxY) {
            for (x in minX..maxX) yield(cellKey(x, y))
        }
    }

    private fun cellKey(x: Int, y: Int): Long =
        (x.toLong() shl 32) xor (y.toLong() and 0xFFFF_FFFFL)
}

object StrokeHitTest {
    fun intersectsPath(stroke: InkStroke, path: List<PagePoint>, radius: Float): Boolean {
        require(radius.isFinite() && radius >= 0f)
        if (path.isEmpty()) return false
        if (path.size == 1) return intersectsCircle(stroke, path.first(), radius)
        val effectiveRadius = radius + stroke.style.maximumWidth / 2f
        return stroke.samples.zipWithNext().any { (strokeA, strokeB) ->
            path.zipWithNext().any { (pathA, pathB) ->
                squaredDistanceBetweenSegments(
                    strokeA.position, strokeB.position, pathA, pathB,
                ) <= effectiveRadius * effectiveRadius
            }
        } || stroke.samples.size == 1 && path.zipWithNext().any { (a, b) ->
            squaredDistanceToSegment(stroke.samples.first().position, a, b) <= effectiveRadius * effectiveRadius
        }
    }

    fun intersectsCircle(stroke: InkStroke, center: PagePoint, radius: Float): Boolean {
        require(radius.isFinite() && radius >= 0f)
        val effectiveRadius = radius + stroke.style.maximumWidth / 2f
        if (!stroke.bounds.expanded(effectiveRadius).intersects(
                PageRect(center.x, center.y, center.x, center.y),
            )
        ) return false

        if (stroke.samples.size == 1) {
            return squaredDistance(stroke.samples.first().position, center) <= effectiveRadius * effectiveRadius
        }
        return stroke.samples.zipWithNext().any { (a, b) ->
            squaredDistanceToSegment(center, a.position, b.position) <= effectiveRadius * effectiveRadius
        }
    }

    fun outsideFragments(stroke: InkStroke, center: PagePoint, radius: Float): List<List<com.mheenotes.core.model.StrokeSample>> {
        val effectiveRadius = radius + stroke.style.maximumWidth / 2f
        val radiusSquared = effectiveRadius * effectiveRadius
        val fragments = mutableListOf<MutableList<com.mheenotes.core.model.StrokeSample>>()
        var current: MutableList<com.mheenotes.core.model.StrokeSample>? = null
        stroke.samples.forEach { sample ->
            if (squaredDistance(sample.position, center) > radiusSquared) {
                val fragment = current ?: mutableListOf<com.mheenotes.core.model.StrokeSample>().also {
                    fragments += it
                    current = it
                }
                fragment += sample
            } else {
                current = null
            }
        }
        return fragments.filter { it.isNotEmpty() }
    }

    private fun squaredDistance(a: PagePoint, b: PagePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun squaredDistanceToSegment(point: PagePoint, start: PagePoint, end: PagePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return squaredDistance(point, start)
        val projection = ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared
        val t = max(0f, min(1f, projection))
        val closest = PagePoint(start.x + t * dx, start.y + t * dy)
        return squaredDistance(point, closest)
    }

    private fun squaredDistanceBetweenSegments(
        a: PagePoint,
        b: PagePoint,
        c: PagePoint,
        d: PagePoint,
    ): Float {
        if (segmentsIntersect(a, b, c, d)) return 0f
        return minOf(
            squaredDistanceToSegment(a, c, d),
            squaredDistanceToSegment(b, c, d),
            squaredDistanceToSegment(c, a, b),
            squaredDistanceToSegment(d, a, b),
        )
    }

    private fun segmentsIntersect(a: PagePoint, b: PagePoint, c: PagePoint, d: PagePoint): Boolean {
        fun cross(p: PagePoint, q: PagePoint, r: PagePoint): Float =
            (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x)
        fun onSegment(p: PagePoint, q: PagePoint, r: PagePoint): Boolean =
            q.x in minOf(p.x, r.x)..maxOf(p.x, r.x) && q.y in minOf(p.y, r.y)..maxOf(p.y, r.y)
        val o1 = cross(a, b, c)
        val o2 = cross(a, b, d)
        val o3 = cross(c, d, a)
        val o4 = cross(c, d, b)
        if (o1 * o2 < 0f && o3 * o4 < 0f) return true
        return o1 == 0f && onSegment(a, c, b) || o2 == 0f && onSegment(a, d, b) ||
            o3 == 0f && onSegment(c, a, d) || o4 == 0f && onSegment(c, b, d)
    }
}
