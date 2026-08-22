package com.mheenotes.app.editor

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class InkRenderOrderTest {
    @Test
    fun highlighterAlwaysRendersBelowWritingInk() {
        val pen = stroke("pen", InkTool.Pen, 1L)
        val pencil = stroke("pencil", InkTool.Pencil, 2L)
        val lateHighlighter = stroke("highlight", InkTool.Highlighter, 30L)

        assertEquals(
            listOf(lateHighlighter, pen, pencil),
            listOf(pencil, lateHighlighter, pen).inInkRenderOrder(),
        )
    }

    private fun stroke(id: String, tool: InkTool, zIndex: Long): InkStroke {
        val sample = StrokeSample(PagePoint(1f, 1f), 0L, .5f, null, null)
        return InkStroke(
            id = StrokeId(id),
            pageId = PageId("page"),
            style = StrokeStyle(tool = tool),
            samples = listOf(sample),
            bounds = PageRect.enclosing(listOf(sample)),
            zIndex = zIndex,
        )
    }
}
