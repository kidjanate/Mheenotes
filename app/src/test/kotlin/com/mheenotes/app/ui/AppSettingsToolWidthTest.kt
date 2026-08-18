package com.mheenotes.app.ui

import com.mheenotes.core.model.InkTool
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsToolWidthTest {
    @Test
    fun changingOneMaximumWidthLeavesTheOtherToolsUnchanged() {
        val settings = AppSettings().withMaximumWidthMicrons(InkTool.Highlighter, 12_000f)

        assertEquals(5_000f, settings.maximumWidthMicrons(InkTool.Pen))
        assertEquals(5_000f, settings.maximumWidthMicrons(InkTool.Pencil))
        assertEquals(12_000f, settings.maximumWidthMicrons(InkTool.Highlighter))
        assertEquals(5_000f, settings.maximumWidthMicrons(InkTool.Eraser))
    }

    @Test
    fun maximumWidthsStayWithinTheSupportedRange() {
        val settings = AppSettings()
            .withMaximumWidthMicrons(InkTool.Pen, 100f)
            .withMaximumWidthMicrons(InkTool.Eraser, 50_000f)

        assertEquals(1_000f, settings.maximumWidthMicrons(InkTool.Pen))
        assertEquals(40_000f, settings.maximumWidthMicrons(InkTool.Eraser))
    }
}
