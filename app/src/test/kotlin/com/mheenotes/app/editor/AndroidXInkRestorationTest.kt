package com.mheenotes.app.editor

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidXInkRestorationTest {
    @Test
    fun normalizesSignedMotionEventAxesForColdStartRendering() {
        val restored = stroke(
            sample(0L, 10f, 20f, pressure = 1.4f, tilt = -.2f, orientation = -1.1f),
            sample(16_000L, 11f, 21f, pressure = .6f, tilt = 2f, orientation = 7.2f),
        ).restoredStrokeInputs()

        assertEquals(1f, restored[0].pressure ?: -1f, 0f)
        assertEquals(0f, restored[0].tiltRadians ?: -1f, 0f)
        assertTrue(restored.all { input ->
            input.orientationRadians?.let { it >= 0f && it < (Math.PI * 2.0).toFloat() } == true
        })
        assertTrue(restored[1].tiltRadians!! <= (Math.PI / 2.0).toFloat())
    }

    @Test
    fun separatesDuplicatePositionAndRoundedTime() {
        val restored = stroke(
            sample(1_000L, 5f, 6f),
            sample(1_001L, 5f, 6f),
            sample(1_002L, 7f, 8f),
        ).restoredStrokeInputs()

        assertEquals(listOf(1L, 2L, 2L), restored.map(RestoredStrokeInput::elapsedTimeMillis))
    }

    @Test
    fun removesPartiallyReportedOptionalAxesFromWholeBatch() {
        val restored = stroke(
            sample(0L, 1f, 1f, pressure = .5f, tilt = .2f, orientation = .3f),
            sample(10_000L, 2f, 2f, pressure = null, tilt = null, orientation = null),
        ).restoredStrokeInputs()

        restored.forEach {
            assertNull(it.pressure)
            assertNull(it.tiltRadians)
            assertNull(it.orientationRadians)
        }
    }

    private fun sample(
        time: Long,
        x: Float,
        y: Float,
        pressure: Float? = .5f,
        tilt: Float? = .2f,
        orientation: Float? = .3f,
    ) = StrokeSample(PagePoint(x, y), time, pressure, tilt, orientation)

    private fun stroke(vararg samples: StrokeSample): InkStroke = InkStroke(
        id = StrokeId("restored-stroke"),
        pageId = PageId("restored-page"),
        style = StrokeStyle(),
        samples = samples.toList(),
        bounds = PageRect.enclosing(samples.toList()),
        zIndex = 0L,
    )
}
