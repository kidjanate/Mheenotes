package com.mheenotes.app.audio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

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
    isRecording: Boolean = false,
) {
    val duration = totalDurationMillis.coerceAtLeast(1f)
    
    val animatedPosition by animateFloatAsState(
        targetValue = currentPositionMillis,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "cursorPosition"
    )
    val progress = (animatedPosition / duration).coerceIn(0f, 1f)

    val safeAmplitudes = remember(amplitudes) {
        if (amplitudes.isNotEmpty()) amplitudes else List(60) { 0.15f }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val recordingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordingScale"
    )
    val currentScale = if (isRecording) recordingScale else 1.0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(currentScale)
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
        val cursorIndex = progress * barCount

        for (i in 0 until barCount) {
            val isGlowing = abs(i - cursorIndex) <= 2f
            val amp = safeAmplitudes[i].coerceIn(0.05f, 1.0f)
            
            val baseHeight = (maxHeight * amp * 0.88f).coerceAtLeast(6f)
            val barHeight = if (isGlowing) baseHeight * 1.1f else baseHeight
            
            val left = i * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val top = (maxHeight - barHeight) / 2f

            var color = if (left <= activeBoundaryX) activeColor else inactiveColor
            if (isGlowing) {
                color = activeColor.copy(alpha = (color.alpha + 0.2f).coerceAtMost(1f))
            }

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
