package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.StrokeSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DrawHoldShapeDetectorTest {
    @Test fun heldLineIsCorrected() {
        val moving = (0..10).map { sample(it * 2_000f, it * 1_000f, it * 30_000L) }
        val held = (1..6).map { sample(20_000f, 10_000f, 300_000L + it * 70_000L) }
        val corrected = DrawHoldShapeDetector.detect(moving + held)
        assertEquals(ShapeKind.Line, corrected?.kind)
        assertEquals(moving.first().position, corrected?.lineStart)
        assertEquals(held.last().position, corrected?.lineEnd)
    }

    @Test fun risingDiagonalKeepsItsDirection() {
        val moving = (0..10).map { sample(it * 2_000f, 20_000f - it * 1_500f, it * 30_000L) }
        val held = (1..6).map { sample(20_000f, 5_000f, 300_000L + it * 70_000L) }
        val corrected = DrawHoldShapeDetector.detect(moving + held)
        assertEquals(ShapeKind.Line, corrected?.kind)
        assertEquals(moving.first().position, corrected?.lineStart)
        assertEquals(held.last().position, corrected?.lineEnd)
    }

    @Test fun nearHorizontalLineSnapsToAxis() {
        val moving = (0..10).map { sample(it * 2_000f, it * 180f, it * 30_000L) }
        val held = (1..6).map { sample(20_000f, 1_800f, 300_000L + it * 70_000L) }

        val corrected = DrawHoldShapeDetector.detect(moving + held)

        assertEquals(ShapeKind.Line, corrected?.kind)
        assertEquals(PagePoint(20_000f, 0f), corrected?.lineEnd)
    }

    @Test fun quickLineIsUntouched() {
        val samples = (0..10).map { sample(it * 2_000f, 0f, it * 10_000L) }
        assertNull(DrawHoldShapeDetector.detect(samples))
    }

    @Test fun lineOnlyModeDoesNotCreateClosedShapes() {
        val moving = listOf(
            sample(0f, 0f, 0L), sample(10_000f, 0f, 40_000L),
            sample(20_000f, 0f, 80_000L), sample(20_000f, 10_000f, 120_000L),
            sample(20_000f, 20_000f, 160_000L), sample(10_000f, 20_000f, 200_000L),
            sample(0f, 20_000f, 240_000L), sample(0f, 10_000f, 280_000L),
            sample(0f, 0f, 320_000L),
        )
        val held = (1..6).map { sample(0f, 0f, 360_000L + it * 70_000L) }

        assertNotNull(DrawHoldShapeDetector.detect(moving + held))
        assertNull(DrawHoldShapeDetector.detect(moving + held, linesOnly = true))
    }

    @Test fun heldRectangleIsRecognized() {
        val moving = polygon(
            PagePoint(0f, 0f),
            PagePoint(24_000f, 0f),
            PagePoint(24_000f, 14_000f),
            PagePoint(0f, 14_000f),
        )
        val held = (1..6).map { sample(0f, 0f, moving.last().elapsedMicros + it * 70_000L) }

        assertEquals(ShapeKind.Rectangle, DrawHoldShapeDetector.detect(moving + held)?.kind)
    }

    @Test fun heldTriangleIsRecognized() {
        val moving = polygon(
            PagePoint(12_000f, 0f),
            PagePoint(24_000f, 18_000f),
            PagePoint(0f, 18_000f),
        )
        val held = (1..6).map {
            sample(12_000f, 0f, moving.last().elapsedMicros + it * 70_000L)
        }

        assertEquals(ShapeKind.Triangle, DrawHoldShapeDetector.detect(moving + held)?.kind)
    }

    private fun polygon(vararg vertices: PagePoint): List<StrokeSample> {
        val closed = vertices.toList() + vertices.first()
        return closed.zipWithNext().flatMapIndexed { edge, (start, end) ->
            (0 until 4).map { step ->
                val amount = step / 4f
                sample(
                    start.x + (end.x - start.x) * amount,
                    start.y + (end.y - start.y) * amount,
                    (edge * 4L + step) * 20_000L,
                )
            }
        } + sample(vertices.first().x, vertices.first().y, closed.size * 80_000L)
    }

    private fun sample(x: Float, y: Float, time: Long) = StrokeSample(PagePoint(x, y), time, .5f, null, null)
}
