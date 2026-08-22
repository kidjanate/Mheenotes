package com.mheenotes.app.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformBar(
    amplitudes: List<Float>,
    currentPositionMillis: Float,
    totalDurationMillis: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val duration = totalDurationMillis.coerceAtLeast(1f)
    val progress = (currentPositionMillis / duration).coerceIn(0f, 1f)

    val safeAmplitudes = remember(amplitudes) {
        if (amplitudes.isNotEmpty()) amplitudes else List(60) { 0.15f }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(safeAmplitudes, duration) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(frac * duration)
                }
            }
            .pointerInput(safeAmplitudes, duration) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(frac * duration)
                }
            },
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barCount = safeAmplitudes.size
        if (barCount == 0 || totalWidth <= 0f) return@Canvas

        val barSlotWidth = totalWidth / barCount
        val barWidth = (barSlotWidth * 0.65f).coerceIn(2f, 8f)
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val activeBoundaryX = totalWidth * progress

        for (i in 0 until barCount) {
            val amp = safeAmplitudes[i].coerceIn(0.05f, 1.0f)
            val barHeight = (maxHeight * amp * 0.88f).coerceAtLeast(6f)
            val left = i * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val top = (maxHeight - barHeight) / 2f

            val color = if (left <= activeBoundaryX) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius,
            )
        }

        // Draw cursor line
        val cursorX = (totalWidth * progress).coerceIn(0f, totalWidth)
        drawLine(
            color = cursorColor,
            start = Offset(cursorX, 0f),
            end = Offset(cursorX, maxHeight),
            strokeWidth = 2.5f.dp.toPx(),
        )
    }
}
