package com.mheenotes.app.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object MheeShapes {
    val pill = RoundedCornerShape(10.dp)
    val card = RoundedCornerShape(14.dp)
    val dialog = RoundedCornerShape(22.dp)
    val toolbar = RoundedCornerShape(16.dp)
    val full = RoundedCornerShape(50)
    val tab = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val thumbnail = RoundedCornerShape(8.dp)
}

object MheeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object MheeMotion {
    const val fast = 150
    const val medium = 280
    const val slow = 400
    const val dialog = 300
    const val staggerDelay = 32L
    val spring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)
    val snappySpring = spring<Float>(dampingRatio = 0.65f, stiffness = 600f)
    val gentleSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
}

object MheeElevation {
    val card = 2.dp
    val toolbar = 6.dp
    val dialog = 16.dp
    val overlay = 24.dp
    val dragGhost = 24.dp
}

object MheeAlpha {
    const val disabled = 0.38f
    const val medium = 0.6f
    const val overlay = 0.08f
    const val scrim = 0.32f
    const val hoverHighlight = 0.06f
}
