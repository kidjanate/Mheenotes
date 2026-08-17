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
    val inputs = MutableStrokeInputBatch()
    samples.forEach { sample ->
        inputs.add(
            StrokeInput.create(
                sample.position.x,
                sample.position.y,
                sample.elapsedMicros / 1_000L,
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
