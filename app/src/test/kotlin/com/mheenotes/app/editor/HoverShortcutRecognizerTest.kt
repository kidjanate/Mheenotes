package com.mheenotes.app.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoverShortcutRecognizerTest {
    @Test
    fun twoHoverImpulsesTriggerWithoutContact() {
        val recognizer = HoverDoubleTapRecognizer(HoverShortcutConfig(2f, 30f, 520L), density = 2f)
        val samples = stableHover(0L, 11) + listOf(
            sample(176, 108f, 100f), sample(192, 100.6f, 100f),
        ) + stableHover(208L, 9) + listOf(
            sample(352, 109f, 101f), sample(368, 100.7f, 100.2f),
        )
        assertTrue(samples.map(recognizer::add).last())
    }

    @Test
    fun ordinaryHoverTravelDoesNotTrigger() {
        val recognizer = HoverDoubleTapRecognizer(HoverShortcutConfig(), density = 2f)
        repeat(20) { index ->
            assertFalse(recognizer.add(sample(index * 16L, index * 4f, index * 2f)))
        }
    }

    @Test
    fun twoCursorJumpsWithoutRecoilDoNotTrigger() {
        val recognizer = HoverDoubleTapRecognizer(HoverShortcutConfig(2f, 30f, 520L), density = 2f)
        val samples = stableHover(0L, 11) + listOf(
            sample(176, 108f, 100f), sample(192, 108.1f, 100f), sample(208, 108.2f, 100f),
            sample(336, 108.1f, 100f), sample(352, 116f, 100f), sample(368, 116.1f, 100f),
        )
        samples.forEach { assertFalse(recognizer.add(it)) }
    }

    @Test
    fun recoilPairsWithLargeAnchorDriftDoNotTrigger() {
        val recognizer = HoverDoubleTapRecognizer(HoverShortcutConfig(2f, 30f, 520L), density = 2f)
        val samples = stableHover(0L, 11) + listOf(
            sample(176, 108f, 100f), sample(192, 100.5f, 100f),
            sample(208, 130f, 100f), sample(224, 138f, 100f), sample(240, 130.5f, 100f),
        )
        samples.forEach { assertFalse(recognizer.add(it)) }
    }

    @Test
    fun calibrationFindsTwoPeaks() {
        val samples = listOf(
            sample(0, 100f, 100f), sample(16, 100.2f, 100f), sample(32, 100.1f, 100f),
            sample(48, 116f, 100f), sample(64, 100.8f, 100f), sample(80, 101f, 100f),
            sample(96, 100.5f, 100f), sample(112, 100.7f, 100f), sample(128, 100.6f, 100f),
            sample(144, 100.8f, 100f), sample(160, 100.7f, 100f), sample(176, 100.8f, 100f),
            sample(192, 100.6f, 100f), sample(208, 117f, 101f), sample(224, 100.8f, 100f),
        )
        assertNotNull(HoverShortcutCalibrator.analyze(samples, density = 2f))
    }

    private fun sample(time: Long, x: Float, y: Float) = StylusHoverSample(time, x, y)

    private fun stableHover(start: Long, count: Int) = List(count) { index ->
        sample(start + index * 16L, 100f + (index % 3) * .15f, 100f)
    }
}
