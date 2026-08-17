package com.mheenotes.ink.engine

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeSpatialIndexTest {
    @Test
    fun queryReturnsOnlyNearbyStroke() {
        val near = stroke("near", 100f, 100f, 500f, 500f)
        val far = stroke("far", 50_000f, 50_000f, 51_000f, 51_000f)
        val index = StrokeSpatialIndex(10_000f).apply { rebuild(listOf(near, far)) }

        assertEquals(listOf(near), index.query(PageRect(0f, 0f, 1_000f, 1_000f)))
    }

    @Test
    fun circleHitUsesExactSegmentDistance() {
        val stroke = stroke("line", 0f, 0f, 10_000f, 0f)

        assertTrue(StrokeHitTest.intersectsCircle(stroke, PagePoint(5_000f, 200f), 300f))
        assertFalse(StrokeHitTest.intersectsCircle(stroke, PagePoint(5_000f, 5_000f), 300f))
    }

    @Test
    fun largeIndexRestrictsQueriesToNearbyCells() {
        val strokes = (0 until 20_000).map { index ->
            val x = (index % 200) * 2_000f
            val y = (index / 200) * 2_000f
            stroke("s$index", x, y, x + 500f, y + 500f)
        }
        val index = StrokeSpatialIndex(5_000f).apply { rebuild(strokes) }
        val result = index.query(PageRect(0f, 0f, 4_000f, 4_000f))
        assertTrue(result.size < 25)
        assertTrue(result.isNotEmpty())
    }

    private fun stroke(id: String, x1: Float, y1: Float, x2: Float, y2: Float): InkStroke {
        val samples = listOf(
            StrokeSample(PagePoint(x1, y1), 0, 0.5f, null, null),
            StrokeSample(PagePoint(x2, y2), 1_000, 0.5f, null, null),
        )
        return InkStroke(
            StrokeId(id),
            PageId("page"),
            StrokeStyle(minimumWidth = 100f, maximumWidth = 100f),
            samples,
            PageRect.enclosing(samples),
            0,
        )
    }
}
