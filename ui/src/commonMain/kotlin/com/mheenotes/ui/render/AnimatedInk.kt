package com.mheenotes.ui.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import com.mheenotes.core.model.InkMotion
import com.mheenotes.core.model.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/** Frames per second for each speed. Stop motion, so deliberately low. */
private val FPS = intArrayOf(4, 8, 12, 16)

/** Displacement multiplier for each strength. */
private val STRENGTH = floatArrayOf(0.6f, 1.2f, 2.2f, 3.5f)

/** Frames in each motion's loop. */
private fun loopLength(motion: Int) = when (motion) {
    InkMotion.WAVE -> 8
    InkMotion.BOUNCE -> 6
    else -> 3
}

/** Milliseconds from [timeMs] until [s] shows its next frame, so pages redraw only when it changes. */
fun animDelayToNextFrame(s: Stroke, timeMs: Long): Long {
    val fps = FPS[InkMotion.unpack(s.anim).speed]
    val tick = timeMs * fps / 1000
    return ((tick + 1) * 1000 + fps - 1) / fps - timeMs
}

/** Furthest an animated stroke moves from its rest position, in page units. */
fun animReach(s: Stroke): Float {
    val m = InkMotion.unpack(s.anim)
    return amplitude(s, m) * if (m.motion == InkMotion.BOUNCE) 2f else 1f
}

private fun amplitude(s: Stroke, m: InkMotion) = (0.5f + s.width * 0.6f) * STRENGTH[m.strength]

/** Deterministic noise in -1..1 for a stroke, frame and knot. */
private fun noise(seed: Int, frame: Int, k: Int): Float {
    var h = seed * 374761393 + frame * 668265263 + k * 1274126177
    h = (h xor (h ushr 13)) * 1103515245
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 32767.5f - 1f
}

/** Smoothly interpolated noise along the stroke, so lines wobble instead of fizz. */
private fun smoothNoise(seed: Int, frame: Int, u: Float): Float {
    val k = u.toInt()
    val f = u - k
    val t = f * f * (3 - 2 * f)
    return noise(seed, frame, k) * (1 - t) + noise(seed, frame, k + 1) * t
}

/**
 * Draws [s] as it looks at [timeMs] in its loop: re-drawn every frame with small variations,
 * like hand-drawn animation. Pure function of the stroke and the clock.
 */
fun DrawScope.drawAnimatedStroke(s: Stroke, timeMs: Long) {
    val m = InkMotion.unpack(s.anim)
    val tick = (timeMs * FPS[m.speed] / 1000).toInt()
    val frame = tick % loopLength(m.motion)
    val seed = s.id.hashCode()
    val amp = amplitude(s, m)
    val src = s.points
    val n = src.size / 3
    val pts = src.copyOf()
    if (n > 0) when (m.motion) {
        InkMotion.WAVE -> {
            // A ripple travels along the stroke, perpendicular to it.
            val wavelength = 18f + s.width * 6f
            val phase = frame / 8f * 2f * PI.toFloat()
            var d = 0f
            for (i in 0 until n) {
                val a = i * 3
                if (i > 0) d += hypot(src[a] - src[a - 3], src[a + 1] - src[a - 2])
                val p = (i - 1).coerceAtLeast(0) * 3
                val q = (i + 1).coerceAtMost(n - 1) * 3
                val tx = src[q] - src[p]; val ty = src[q + 1] - src[p + 1]
                val len = hypot(tx, ty).takeIf { it > 1e-4f } ?: continue
                val off = amp * sin(d / wavelength * 2f * PI.toFloat() - phase)
                pts[a] += -ty / len * off
                pts[a + 1] += tx / len * off
            }
        }
        InkMotion.BOUNCE -> {
            // The whole stroke hops, with a little jiggle so each frame is still hand-made.
            val hop = -2f * amp * abs(sin(frame / 6f * PI.toFloat()))
            val jx = noise(seed, frame, 7) * amp * 0.25f
            for (i in 0 until n) { pts[i * 3] += jx; pts[i * 3 + 1] += hop }
        }
        else -> {
            // Boil: every frame redraws the line with a different gentle wobble.
            var d = 0f
            for (i in 0 until n) {
                val a = i * 3
                if (i > 0) d += hypot(src[a] - src[a - 3], src[a + 1] - src[a - 2])
                val u = d / 22f
                pts[a] += smoothNoise(seed, frame * 2, u) * amp
                pts[a + 1] += smoothNoise(seed xor 0x5bd1e995, frame * 2 + 1, u) * amp
            }
        }
    }
    val argb = when (m.color) {
        InkMotion.RAINBOW -> Color.hsv(((tick * 40 + (seed and 0xff)) % 360).toFloat(), 0.72f, 0.92f).toArgb()
        InkMotion.FLICKER -> {
            // Brighten rather than fade: translucent ink would show its overlapping joints.
            val k = 0.55f * (noise(seed, tick, 3) * 0.5f + 0.5f)
            val c = Color(s.argb)
            Color(c.red + (1 - c.red) * k, c.green + (1 - c.green) * k, c.blue + (1 - c.blue) * k).toArgb()
        }
        else -> s.argb
    }
    // Width breathes a little per frame too.
    val width = s.width * (1f + 0.08f * noise(seed, tick, 11))
    drawInkStroke(Stroke(s.id, s.seq, s.tool, argb, width, pts, s.anim))
}
