package com.mheenotes.ink.engine

import com.mheenotes.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoSelectionTest {
    private val polygon = listOf(PagePoint(0f, 0f), PagePoint(100f, 0f), PagePoint(100f, 100f), PagePoint(0f, 100f))

    @Test fun pointContainmentAndStrokeSelection() {
        assertTrue(LassoSelection.contains(polygon, PagePoint(50f, 50f)))
        val sample = StrokeSample(PagePoint(50f, 50f), 0, null, null, null)
        val stroke = InkStroke(StrokeId("s"), PageId("p"), StrokeStyle(), listOf(sample), PageRect.enclosing(listOf(sample)), 0)
        assertEquals(listOf(stroke), LassoSelection.select(listOf(stroke), polygon))
    }

    @Test fun transformMovesEditableSamples() {
        val sample = StrokeSample(PagePoint(10f, 10f), 0, null, null, null)
        val stroke = InkStroke(StrokeId("s"), PageId("p"), StrokeStyle(), listOf(sample), PageRect.enclosing(listOf(sample)), 0)
        val moved = LassoSelection.transform(stroke, SelectionTransform(5f, 7f, pivot = PagePoint(0f, 0f)))
        assertEquals(PagePoint(15f, 17f), moved.samples.single().position)
    }
}
