package com.mheenotes.ink.engine

import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeBuilderTest {
    @Test
    fun ignoresOutOfOrderSamples() {
        val builder = StrokeBuilder(
            StrokeId("stroke"),
            PageId("page"),
            StrokeStyle(),
            zIndex = 0,
            smoothing = 0f,
        )

        builder.append(StrokeSample(PagePoint(1f, 1f), 10, 0.5f, null, null))
        builder.append(StrokeSample(PagePoint(2f, 2f), 5, 0.5f, null, null))

        assertEquals(1, builder.finish().samples.size)
    }
}

