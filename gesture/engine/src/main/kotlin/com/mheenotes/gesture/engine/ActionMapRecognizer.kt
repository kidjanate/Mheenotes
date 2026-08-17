package com.mheenotes.gesture.engine

import com.mheenotes.core.model.ActionMapping
import com.mheenotes.core.model.GestureDrawing
import com.mheenotes.core.model.GesturePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class ActionMatch(val mapping: ActionMapping, val confidence: Float)

class ActionMapRecognizer(private val pointsPerStroke: Int = 32) {
    init {
        require(pointsPerStroke >= 8)
    }

    fun recognize(input: GestureDrawing, mappings: List<ActionMapping>): ActionMatch? {
        var best: ActionMatch? = null
        mappings.forEach { mapping ->
            val confidence = mapping.examples.maxOfOrNull { example ->
                similarity(input, example, mapping.rotationInvariant)
            } ?: 0f
            if (confidence >= mapping.confidenceThreshold && confidence > (best?.confidence ?: -1f)) {
                best = ActionMatch(mapping, confidence)
            }
        }
        return best
    }

    fun similarity(a: GestureDrawing, b: GestureDrawing, rotationInvariant: Boolean): Float {
        if (a.strokes.size != b.strokes.size) return 0f
        val left = normalize(a, rotationInvariant)
        val right = normalize(b, rotationInvariant)
        val forward = rootMeanSquareDistance(left, right)
        val reversed = rootMeanSquareDistance(left, right.map { it.asReversed() })
        val distance = minOf(forward, reversed)
        return (1f - distance / 0.72f).coerceIn(0f, 1f)
    }

    private fun normalize(drawing: GestureDrawing, rotationInvariant: Boolean): List<List<GesturePoint>> {
        var strokes = drawing.strokes.map { resample(it, pointsPerStroke) }
        val all = strokes.flatten()
        val centerX = all.sumOf { it.x.toDouble() }.toFloat() / all.size
        val centerY = all.sumOf { it.y.toDouble() }.toFloat() / all.size
        strokes = strokes.map { stroke -> stroke.map { GesturePoint(it.x - centerX, it.y - centerY) } }
        if (rotationInvariant) {
            val first = strokes.first().first()
            val angle = -atan2(first.y, first.x)
            val c = cos(angle)
            val s = sin(angle)
            strokes = strokes.map { stroke ->
                stroke.map { GesturePoint(it.x * c - it.y * s, it.x * s + it.y * c) }
            }
        }
        val flat = strokes.flatten()
        val width = (flat.maxOf { it.x } - flat.minOf { it.x }).coerceAtLeast(1e-4f)
        val height = (flat.maxOf { it.y } - flat.minOf { it.y }).coerceAtLeast(1e-4f)
        val scale = max(width, height)
        return strokes.map { stroke -> stroke.map { GesturePoint(it.x / scale, it.y / scale) } }
    }

    private fun resample(points: List<GesturePoint>, count: Int): List<GesturePoint> {
        if (points.size == 1) return List(count) { points.first() }
        val lengths = points.zipWithNext().map { (a, b) -> hypot(b.x - a.x, b.y - a.y) }
        val total = lengths.sum()
        if (total <= 1e-5f) return List(count) { points.first() }
        val result = ArrayList<GesturePoint>(count)
        var segment = 0
        var consumed = 0f
        repeat(count) { index ->
            val target = total * index / (count - 1f)
            while (segment < lengths.lastIndex && consumed + lengths[segment] < target) {
                consumed += lengths[segment++]
            }
            val start = points[segment]
            val end = points[segment + 1]
            val t = ((target - consumed) / lengths[segment].coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
            result += GesturePoint(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
        }
        return result
    }

    private fun rootMeanSquareDistance(a: List<List<GesturePoint>>, b: List<List<GesturePoint>>): Float {
        var squared = 0f
        var count = 0
        a.indices.forEach { strokeIndex ->
            a[strokeIndex].indices.forEach { pointIndex ->
                val dx = a[strokeIndex][pointIndex].x - b[strokeIndex][pointIndex].x
                val dy = a[strokeIndex][pointIndex].y - b[strokeIndex][pointIndex].y
                squared += dx * dx + dy * dy
                count++
            }
        }
        return sqrt(squared / count.coerceAtLeast(1))
    }
}
