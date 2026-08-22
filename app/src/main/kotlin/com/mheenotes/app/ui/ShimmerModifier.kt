package com.mheenotes.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-offset",
    )
    drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.4f to Color.White.copy(alpha = 0.07f),
                    0.5f to Color.White.copy(alpha = 0.12f),
                    0.6f to Color.White.copy(alpha = 0.07f),
                    1f to Color.Transparent,
                ),
                start = Offset(size.width * offset, 0f),
                end = Offset(size.width * (offset + 1f), size.height),
            ),
        )
    }
}

fun Modifier.shimmerPlaceholder(visible: Boolean): Modifier = if (visible) {
    this
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White.copy(alpha = 0.04f))
        .shimmer()
} else this
