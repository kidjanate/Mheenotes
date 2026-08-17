package com.mheenotes.ink.engine

import com.mheenotes.core.model.PressureCurve
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class PressureNormalizerTest {
    @Test
    fun clampsPressureOutsideCalibratedRange() {
        val curve = PressureCurve(0.1f, 0.9f, 1f)

        assertEquals(0f, PressureNormalizer.normalize(-1f, curve), 0.0001f)
        assertEquals(1f, PressureNormalizer.normalize(2f, curve), 0.0001f)
    }

    @Test
    fun disabledPressureUsesStableMidpointWidth() {
        val style = StrokeStyle(
            minimumWidth = 2f,
            maximumWidth = 6f,
            pressureEnabled = false,
        )

        assertEquals(4f, PressureNormalizer.width(0f, style), 0.0001f)
        assertEquals(4f, PressureNormalizer.width(1f, style), 0.0001f)
    }
}

