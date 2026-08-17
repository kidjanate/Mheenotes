package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.StrokeSample
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

enum class ScribbleSensitivity { Low, Medium, High }

data class ScribbleMetrics(
    val pathToDiagonalRatio: Float,
    val sharpTurns: Int,
    val selfIntersections: Int,
    val directionReversals: Int,
)

object ScribbleEraseDetector {
    fun isScribble(samples: List<StrokeSample>, sensitivity: ScribbleSensitivity): Boolean {
        if (samples.size < 12) return false
        val points = samples.map { it.position }
        val preliminary = measureWithoutIntersections(points) ?: return false
        val thresholds = when (sensitivity) {
            ScribbleSensitivity.Low -> Thresholds(5.4f, 8, 2, 5)
            ScribbleSensitivity.Medium -> Thresholds(4.3f, 6, 1, 2)
            ScribbleSensitivity.High -> Thresholds(3.5f, 5, 1, 3)
        }
        if (preliminary.pathToDiagonalRatio < thresholds.pathRatio ||
            preliminary.sharpTurns < thresholds.turns ||
            preliminary.directionReversals < thresholds.reversals
        ) return false
        return countSelfIntersections(points) >= thresholds.intersections
    }

    fun measure(points: List<PagePoint>): ScribbleMetrics? {
        val preliminary = measureWithoutIntersections(points) ?: return null
        return preliminary.copy(selfIntersections = countSelfIntersections(points))
    }

    private fun measureWithoutIntersections(points: List<PagePoint>): ScribbleMetrics? {
        if (points.size < 3) return null
        val width = points.maxOf { it.x } - points.minOf { it.x }
        val height = points.maxOf { it.y } - points.minOf { it.y }
        val diagonal = hypot(width, height)
        if (diagonal < 2_500f || minOf(width, height) < 700f) return null
        val pathLength = points.zipWithNext().sumOf { (a, b) ->
            hypot(b.x - a.x, b.y - a.y).toDouble()
        }.toFloat()
        var sharpTurns = 0
        var reversals = 0
        var previousTurnSign = 0
        points.windowed(3).forEach { (a, b, c) ->
            val first = atan2(b.y - a.y, b.x - a.x)
            val second = atan2(c.y - b.y, c.x - b.x)
            var turn = second - first
            while (turn > PI) turn -= (2 * PI).toFloat()
            while (turn < -PI) turn += (2 * PI).toFloat()
            if (abs(turn) > 0.72f) sharpTurns++
            val sign = when { turn > 0.42f -> 1; turn < -0.42f -> -1; else -> 0 }
            if (sign != 0 && previousTurnSign != 0 && sign != previousTurnSign) reversals++
            if (sign != 0) previousTurnSign = sign
        }
        return ScribbleMetrics(pathLength / diagonal, sharpTurns, 0, reversals)
    }

    private fun countSelfIntersections(source: List<PagePoint>): Int {
        val stride = (source.size / 160).coerceAtLeast(1)
        val points = source.filterIndexed { index, _ -> index % stride == 0 }.toMutableList().apply {
            if (lastOrNull() != source.last()) add(source.last())
        }
        var intersections = 0
        for (first in 0 until points.lastIndex) {
            for (second in first + 3 until points.lastIndex) {
                if (segmentsIntersect(points[first], points[first + 1], points[second], points[second + 1])) {
                    intersections++
                    if (intersections >= 12) return intersections
                }
            }
        }
        return intersections
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

    private data class Thresholds(
        val pathRatio: Float,
        val turns: Int,
        val intersections: Int,
        val reversals: Int,
    )
}
