package com.mheenotes.core.commands

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageHistoryTest {
    private val pageId = PageId("page")
    private val sample = StrokeSample(PagePoint(1f, 2f), 0, 0.5f, null, null)
    private val stroke = InkStroke(
        id = StrokeId("stroke"),
        pageId = pageId,
        style = StrokeStyle(),
        samples = listOf(sample),
        bounds = PageRect.enclosing(listOf(sample)),
        zIndex = 0,
    )

    @Test
    fun executeUndoAndRedoPreserveStroke() {
        val history = PageHistory(PageState(pageId))

        history.execute(AddStroke(stroke))
        assertEquals(listOf(stroke), history.current.strokes)
        assertTrue(history.canUndo)

        history.undo()
        assertTrue(history.current.strokes.isEmpty())
        assertTrue(history.canRedo)

        history.redo()
        assertEquals(listOf(stroke), history.current.strokes)
        assertFalse(history.canRedo)
    }

    @Test
    fun batchReplacementIsOneUndoStep() {
        val second = stroke.copy(id = StrokeId("second"), zIndex = 1)
        val moved = stroke.copy(
            id = StrokeId("moved"),
            samples = listOf(sample.copy(position = PagePoint(10f, 20f))),
            bounds = PageRect(10f, 20f, 10f, 20f),
        )
        val history = PageHistory(PageState(pageId, strokes = listOf(stroke, second)))
        history.execute(ReplaceStrokes(setOf(stroke.id), listOf(moved)))
        assertEquals(listOf(moved, second), history.current.strokes)
        history.undo()
        assertEquals(listOf(stroke, second), history.current.strokes)
    }
}
