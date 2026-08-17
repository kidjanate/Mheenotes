package com.mheenotes.gesture.engine

import com.mheenotes.core.model.ActionMapping
import com.mheenotes.core.model.GestureDrawing
import com.mheenotes.core.model.GesturePoint
import com.mheenotes.core.model.MappedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionMapRecognizerTest {
    private val recognizer = ActionMapRecognizer()

    @Test
    fun recognizesScaledTranslatedMultiStrokeGesture() {
        val mapping = mapping(false, listOf(h(0f), h(2f), h(-2f)))
        val match = recognizer.recognize(h(20f, scale = 4f), listOf(mapping))
        assertEquals(MappedAction.Undo, match?.mapping?.action)
        assertTrue((match?.confidence ?: 0f) > .9f)
    }

    @Test
    fun rejectsWrongStrokeCount() {
        val mapping = mapping(false, listOf(h(0f), h(1f), h(2f)))
        assertNull(recognizer.recognize(GestureDrawing(listOf(line(0f, 0f, 10f, 10f))), listOf(mapping)))
    }

    @Test
    fun rotationInvariantMappingRecognizesRotatedDrawing() {
        val mapping = mapping(true, listOf(h(0f), h(1f), h(2f)))
        val rotated = GestureDrawing(h(0f).strokes.map { stroke -> stroke.map { GesturePoint(-it.y, it.x) } })
        assertEquals(MappedAction.Undo, recognizer.recognize(rotated, listOf(mapping))?.mapping?.action)
    }

    private fun mapping(rotation: Boolean, examples: List<GestureDrawing>) = ActionMapping(
        "undo-h", "Undo H", MappedAction.Undo, examples, rotation, .72f,
    )

    private fun h(offset: Float, scale: Float = 1f) = GestureDrawing(listOf(
        line(offset, 0f, offset, 10f * scale),
        line(offset + 10f * scale, 0f, offset + 10f * scale, 10f * scale),
        line(offset, 5f * scale, offset + 10f * scale, 5f * scale),
    ))

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        (0..10).map { index ->
            val t = index / 10f
            GesturePoint(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
        }
}
