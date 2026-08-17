package com.mheenotes.core.model

import kotlin.math.max
import kotlin.math.min

@JvmInline
value class StrokeId(val value: String) {
    init {
        require(value.isNotBlank()) { "Stroke ID cannot be blank" }
    }
}

@JvmInline
value class PageId(val value: String) {
    init {
        require(value.isNotBlank()) { "Page ID cannot be blank" }
    }
}

data class PagePoint(
    val x: Float,
    val y: Float,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Page coordinates must be finite" }
    }
}

data class PageRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(listOf(left, top, right, bottom).all(Float::isFinite)) {
            "Bounds must be finite"
        }
        require(left <= right && top <= bottom) { "Bounds must not be inverted" }
    }

    companion object {
        fun enclosing(points: List<StrokeSample>): PageRect {
            require(points.isNotEmpty()) { "Cannot calculate empty stroke bounds" }

            var left = points.first().position.x
            var top = points.first().position.y
            var right = left
            var bottom = top
            points.drop(1).forEach { sample ->
                left = min(left, sample.position.x)
                top = min(top, sample.position.y)
                right = max(right, sample.position.x)
                bottom = max(bottom, sample.position.y)
            }
            return PageRect(left, top, right, bottom)
        }
    }

    fun intersects(other: PageRect): Boolean =
        left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

    fun expanded(amount: Float): PageRect {
        require(amount.isFinite() && amount >= 0f)
        return PageRect(left - amount, top - amount, right + amount, bottom + amount)
    }
}

data class StrokeSample(
    val position: PagePoint,
    val elapsedMicros: Long,
    val pressure: Float?,
    val tiltRadians: Float?,
    val orientationRadians: Float?,
) {
    init {
        require(elapsedMicros >= 0) { "Stroke time cannot be negative" }
        require(pressure == null || pressure.isFinite()) { "Pressure must be finite" }
        require(tiltRadians == null || tiltRadians.isFinite()) { "Tilt must be finite" }
        require(orientationRadians == null || orientationRadians.isFinite()) {
            "Orientation must be finite"
        }
    }
}

enum class InkTool {
    Pen,
    Pencil,
    Highlighter,
    Eraser,
}

data class PressureCurve(
    val minimumUsefulPressure: Float = 0.02f,
    val maximumUsefulPressure: Float = 0.85f,
    val exponent: Float = 0.72f,
) {
    init {
        require(minimumUsefulPressure.isFinite() && minimumUsefulPressure >= 0f)
        require(maximumUsefulPressure.isFinite() && maximumUsefulPressure > minimumUsefulPressure)
        require(exponent.isFinite() && exponent > 0f)
    }
}

data class StrokeStyle(
    val tool: InkTool = InkTool.Pen,
    val argb: Int = 0xFF17212B.toInt(),
    val minimumWidth: Float = 600f,
    val maximumWidth: Float = 1_400f,
    val opacity: Float = 1f,
    val pressureEnabled: Boolean = true,
    val pressureCurve: PressureCurve = PressureCurve(),
) {
    init {
        require(minimumWidth.isFinite() && minimumWidth > 0f)
        require(maximumWidth.isFinite() && maximumWidth >= minimumWidth)
        require(opacity.isFinite() && opacity in 0f..1f)
    }
}

data class InkStroke(
    val id: StrokeId,
    val pageId: PageId,
    val style: StrokeStyle,
    val samples: List<StrokeSample>,
    val bounds: PageRect,
    val zIndex: Long,
) {
    init {
        require(samples.isNotEmpty()) { "A stroke must contain at least one sample" }
        require(samples.zipWithNext().all { (a, b) -> a.elapsedMicros <= b.elapsedMicros }) {
            "Stroke samples must be ordered by time"
        }
    }
}

data class PageState(
    val id: PageId,
    val strokes: List<InkStroke> = emptyList(),
    val revision: Long = 0,
)
