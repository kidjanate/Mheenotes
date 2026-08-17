package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.StrokeSample
import kotlin.math.abs
import kotlin.math.hypot

data class CorrectedShape(
    val kind: ShapeKind,
    val bounds: PageRect,
    val lineStart: PagePoint? = null,
    val lineEnd: PagePoint? = null,
)

/** Conservative release-time recognizer. It only activates after a stationary end hold. */
object DrawHoldShapeDetector {
    fun detect(samples: List<StrokeSample>): CorrectedShape? {
        if (samples.size < 8) return null
        val last = samples.last()
        val holdStartTime = last.elapsedMicros - 320_000L
        val holdSamples = samples.filter { it.elapsedMicros >= holdStartTime }
        if (holdSamples.size < 2 || last.elapsedMicros - samples.first().elapsedMicros < 420_000L) return null
        if (holdSamples.maxOf { distance(it.position, last.position) } > 2_200f) return null
        val moving = samples.takeWhile { it.elapsedMicros < holdStartTime }.ifEmpty { return null }
        if (moving.size < 5) return null
        val bounds = PageRect(
            moving.minOf { it.position.x }, moving.minOf { it.position.y },
            moving.maxOf { it.position.x }, moving.maxOf { it.position.y },
        )
        val diagonal = hypot(bounds.right - bounds.left, bounds.bottom - bounds.top)
        if (diagonal < 8_000f) return null
        val first = moving.first().position
        val end = last.position
        val deviations = moving.map { pointLineDistance(it.position, first, end) }
        val meanDeviation = deviations.average().toFloat()
        val maximumDeviation = deviations.maxOrNull() ?: Float.MAX_VALUE
        if (
            meanDeviation < maxOf(1_400f, diagonal * .055f) &&
            maximumDeviation < maxOf(3_200f, diagonal * .12f)
        ) return CorrectedShape(ShapeKind.Line, bounds, first, end)
        val closed = distance(first, end) < diagonal * .24f
        if (!closed) return null
        val center = PagePoint((bounds.left + bounds.right) / 2f, (bounds.top + bounds.bottom) / 2f)
        val radii = moving.map { distance(it.position, center) }
        val average = radii.average().toFloat().coerceAtLeast(1f)
        val radialError = radii.sumOf { abs(it - average).toDouble() }.toFloat() / radii.size / average
        val kind = if (radialError < .18f) ShapeKind.Ellipse else ShapeKind.Rectangle
        return CorrectedShape(kind, bounds)
    }

    private fun distance(a: PagePoint, b: PagePoint) = hypot(a.x - b.x, a.y - b.y)

    private fun pointLineDistance(point: PagePoint, start: PagePoint, end: PagePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 1f) return distance(point, start)
        return abs(dy * point.x - dx * point.y + end.x * start.y - end.y * start.x) / length
    }
}
