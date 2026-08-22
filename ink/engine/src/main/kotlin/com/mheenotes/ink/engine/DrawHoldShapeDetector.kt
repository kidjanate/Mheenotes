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
    fun detect(samples: List<StrokeSample>, linesOnly: Boolean = false): CorrectedShape? {
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
        ) return CorrectedShape(ShapeKind.Line, bounds, first, snapToAxis(first, end))
        if (linesOnly) return null
        val closed = distance(first, end) < diagonal * .24f
        if (!closed) return null

        val contour = moving.map(StrokeSample::position) + end
        val vertices = simplifiedClosedVertices(contour, maxOf(900f, diagonal * .055f))
        if (vertices.size == 3) return CorrectedShape(ShapeKind.Triangle, bounds)
        if (vertices.size == 4) return CorrectedShape(ShapeKind.Rectangle, bounds)

        val center = PagePoint((bounds.left + bounds.right) / 2f, (bounds.top + bounds.bottom) / 2f)
        val radiusX = ((bounds.right - bounds.left) / 2f).coerceAtLeast(1f)
        val radiusY = ((bounds.bottom - bounds.top) / 2f).coerceAtLeast(1f)
        val ellipseError = moving.sumOf { sample ->
            abs(
                hypot(
                    (sample.position.x - center.x) / radiusX,
                    (sample.position.y - center.y) / radiusY,
                ) - 1f,
            ).toDouble()
        }.toFloat() / moving.size
        return CorrectedShape(ShapeKind.Ellipse, bounds).takeIf { ellipseError < .18f }
    }

    private fun snapToAxis(start: PagePoint, end: PagePoint): PagePoint {
        val dx = abs(end.x - start.x)
        val dy = abs(end.y - start.y)
        val axisTolerance = .14f // About eight degrees from horizontal or vertical.
        return when {
            dy <= dx * axisTolerance -> PagePoint(end.x, start.y)
            dx <= dy * axisTolerance -> PagePoint(start.x, end.y)
            else -> end
        }
    }

    private fun simplifiedClosedVertices(points: List<PagePoint>, tolerance: Float): List<PagePoint> {
        if (points.size < 4) return emptyList()
        val first = points.first()
        val splitIndex = (1 until points.lastIndex).maxByOrNull { distance(first, points[it]) }
            ?: return emptyList()
        if (splitIndex <= 0 || splitIndex >= points.lastIndex) return emptyList()
        val firstHalf = simplify(points.subList(0, splitIndex + 1), tolerance)
        val secondHalf = simplify(points.subList(splitIndex, points.size), tolerance)
        return firstHalf.dropLast(1) + secondHalf.dropLast(1)
    }

    private fun simplify(points: List<PagePoint>, tolerance: Float): List<PagePoint> {
        if (points.size <= 2) return points
        val first = points.first()
        val last = points.last()
        var farthestIndex = -1
        var farthestDistance = 0f
        for (index in 1 until points.lastIndex) {
            val deviation = pointLineDistance(points[index], first, last)
            if (deviation > farthestDistance) {
                farthestDistance = deviation
                farthestIndex = index
            }
        }
        if (farthestIndex < 0 || farthestDistance <= tolerance) return listOf(first, last)
        val left = simplify(points.subList(0, farthestIndex + 1), tolerance)
        val right = simplify(points.subList(farthestIndex, points.size), tolerance)
        return left.dropLast(1) + right
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
