package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.StrokeSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScribbleEraseDetectorTest {
    @Test
    fun repeatedCrossingStrokeIsScribble() {
        val anchors = buildList {
            repeat(5) {
                add(PagePoint(0f, 0f))
                add(PagePoint(12_000f, 10_000f))
                add(PagePoint(0f, 10_000f))
                add(PagePoint(12_000f, 0f))
            }
        }
        val samples = densify(anchors)
        assertTrue(
            ScribbleEraseDetector.measure(samples.map { it.position }).toString(),
            ScribbleEraseDetector.isScribble(samples, ScribbleSensitivity.Medium),
        )
    }

    @Test
    fun straightLineIsNeverScribble() {
        val samples = (0..30).map { sample(it * 1_000f, it * 100f, it) }
        assertFalse(ScribbleEraseDetector.isScribble(samples, ScribbleSensitivity.High))
    }

    @Test
    fun ordinarySingleLoopIsNotScribble() {
        val samples = (0..30).map { index ->
            val angle = index * Math.PI * 2 / 30
            sample((10_000 + kotlin.math.cos(angle) * 8_000).toFloat(), (10_000 + kotlin.math.sin(angle) * 8_000).toFloat(), index)
        }
        assertFalse(ScribbleEraseDetector.isScribble(samples, ScribbleSensitivity.Medium))
    }

    private fun densify(points: List<PagePoint>): List<StrokeSample> = points.zipWithNext().flatMapIndexed { index, (a, b) ->
        (0 until 4).map { part ->
            val t = part / 4f
            sample(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, index * 4 + part)
        }
    } + sample(points.last().x, points.last().y, points.size * 4)

    private fun sample(x: Float, y: Float, index: Int) = StrokeSample(
        PagePoint(x, y), index.toLong() * 1_000, .5f, null, null,
    )
}
