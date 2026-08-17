package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.StrokeSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeSmootherTest {
    private val samples = listOf(
        StrokeSample(PagePoint(0f, 0f), 0, null, null, null),
        StrokeSample(PagePoint(10f, 10f), 1, null, null, null),
        StrokeSample(PagePoint(20f, 0f), 2, null, null, null),
    )

    @Test
    fun rawReturnsOriginalSamples() {
        assertEquals(samples, StrokeSmoother.smooth(samples, SmoothingLevel.Raw))
    }

    @Test
    fun smoothingPreservesEndpointsAndMovesInteriorPoint() {
        val smoothed = StrokeSmoother.smooth(samples, SmoothingLevel.High)
        assertEquals(samples.first(), smoothed.first())
        assertEquals(samples.last(), smoothed.last())
        assertTrue(smoothed[1].position.x < samples[1].position.x)
    }
}

