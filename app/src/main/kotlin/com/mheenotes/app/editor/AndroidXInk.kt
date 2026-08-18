package com.mheenotes.app.editor

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.StrokeStyle
import kotlin.math.PI

internal data class RestoredStrokeInput(
    val x: Float,
    val y: Float,
    val elapsedTimeMillis: Long,
    val pressure: Float?,
    val tiltRadians: Float?,
    val orientationRadians: Float?,
)

fun StrokeStyle.toAndroidXBrush(): Brush {
    val family = when (tool) {
        InkTool.Pen -> if (pressureEnabled) StockBrushes.pressurePen() else StockBrushes.marker()
        InkTool.Pencil -> StockBrushes.pressurePen()
        InkTool.Highlighter -> StockBrushes.highlighter()
        InkTool.Eraser -> StockBrushes.marker()
    }
    val size = if (pressureEnabled) maximumWidth else (minimumWidth + maximumWidth) / 2f
    val colorWithOpacity = (argb and 0x00FF_FFFF) or
        ((opacity * 255).toInt().coerceIn(0, 255) shl 24)
    return Brush.createWithColorIntArgb(
        family = family,
        colorIntArgb = colorWithOpacity,
        size = size,
        epsilon = 80f,
    )
}

fun InkStroke.toAndroidXStroke(): Stroke {
    val restoredInputs = restoredStrokeInputs()
    val inputs = MutableStrokeInputBatch()
    restoredInputs.forEach { sample ->
        inputs.add(
            StrokeInput.create(
                sample.x,
                sample.y,
                sample.elapsedTimeMillis,
                InputToolType.STYLUS,
                StrokeInput.NO_STROKE_UNIT_LENGTH,
                sample.pressure ?: StrokeInput.NO_PRESSURE,
                sample.tiltRadians ?: StrokeInput.NO_TILT,
                sample.orientationRadians ?: StrokeInput.NO_ORIENTATION,
            ),
        )
    }
    return Stroke(style.toAndroidXBrush(), inputs)
}

/**
 * Converts persisted Android MotionEvent samples to AndroidX Ink's stricter input contract.
 *
 * Android reports orientation in a signed range while AndroidX Ink requires [0, 2π). The
 * journal also retains microseconds, but AndroidX Ink stores milliseconds and rejects an input
 * when both its rounded time and position duplicate another input. Either issue used to make
 * restoration throw, causing the entire otherwise-valid stroke to be skipped after a cold start.
 */
internal fun InkStroke.restoredStrokeInputs(): List<RestoredStrokeInput> {
    val keepPressure = samples.all { it.pressure != null }
    val keepTilt = samples.all { it.tiltRadians != null }
    val keepOrientation = samples.all { it.orientationRadians != null }
    val usedPositionTimes = hashSetOf<RestoredPositionTime>()
    var lastTimeMillis = 0L
    return samples.map { sample ->
        var timeMillis = maxOf(sample.elapsedMicros / 1_000L, lastTimeMillis)
        val canonicalX = sample.position.x.takeUnless { it == 0f } ?: 0f
        val canonicalY = sample.position.y.takeUnless { it == 0f } ?: 0f
        var key = RestoredPositionTime(canonicalX, canonicalY, timeMillis)
        while (!usedPositionTimes.add(key)) {
            timeMillis++
            key = key.copy(elapsedTimeMillis = timeMillis)
        }
        lastTimeMillis = timeMillis
        RestoredStrokeInput(
            x = sample.position.x,
            y = sample.position.y,
            elapsedTimeMillis = timeMillis,
            pressure = sample.pressure?.coerceIn(0f, 1f).takeIf { keepPressure },
            tiltRadians = sample.tiltRadians?.coerceIn(0f, (PI / 2.0).toFloat()).takeIf { keepTilt },
            orientationRadians = sample.orientationRadians?.normalizedOrientation().takeIf { keepOrientation },
        )
    }
}

private data class RestoredPositionTime(
    val x: Float,
    val y: Float,
    val elapsedTimeMillis: Long,
)

private fun Float.normalizedOrientation(): Float {
    val fullTurn = (PI * 2.0).toFloat()
    val wrapped = this % fullTurn
    return if (wrapped < 0f) wrapped + fullTurn else wrapped
}
