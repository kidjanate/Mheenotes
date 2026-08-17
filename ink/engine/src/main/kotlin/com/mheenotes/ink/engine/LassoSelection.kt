package com.mheenotes.ink.engine

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import kotlin.math.cos
import kotlin.math.sin

data class SelectionTransform(
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationRadians: Float = 0f,
    val pivot: PagePoint,
)

object LassoSelection {
    fun select(strokes: Collection<InkStroke>, polygon: List<PagePoint>): List<InkStroke> {
        require(polygon.size >= 3)
        val bounds = PageRect(
            polygon.minOf { it.x }, polygon.minOf { it.y },
            polygon.maxOf { it.x }, polygon.maxOf { it.y },
        )
        return strokes.filter { stroke ->
            stroke.bounds.intersects(bounds) && stroke.samples.any { contains(polygon, it.position) }
        }
    }

    fun transform(stroke: InkStroke, value: SelectionTransform): InkStroke {
        val c = cos(value.rotationRadians)
        val s = sin(value.rotationRadians)
        val samples = stroke.samples.map { sample ->
            val x = (sample.position.x - value.pivot.x) * value.scaleX
            val y = (sample.position.y - value.pivot.y) * value.scaleY
            sample.copy(position = PagePoint(
                x = value.pivot.x + x * c - y * s + value.translateX,
                y = value.pivot.y + x * s + y * c + value.translateY,
            ))
        }
        return stroke.copy(samples = samples, bounds = PageRect.enclosing(samples))
    }

    fun contains(polygon: List<PagePoint>, point: PagePoint): Boolean {
        var inside = false
        var previous = polygon.last()
        polygon.forEach { current ->
            val crosses = (current.y > point.y) != (previous.y > point.y) &&
                point.x < (previous.x - current.x) * (point.y - current.y) /
                (previous.y - current.y) + current.x
            if (crosses) inside = !inside
            previous = current
        }
        return inside
    }
}
