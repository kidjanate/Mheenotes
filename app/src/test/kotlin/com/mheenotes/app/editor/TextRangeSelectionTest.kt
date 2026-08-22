package com.mheenotes.app.editor

import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextRangeSelectionTest {
    private val tokens = listOf(
        token("Hello", 0f, 0f, 10f, 10f, line = 0),
        token("world", 12f, 0f, 25f, 10f, line = 0),
        token("Next", 0f, 15f, 10f, 25f, line = 1),
        token("line", 12f, 15f, 22f, 25f, line = 1),
    )

    @Test
    fun selectionReconstructsWordsAcrossLines() {
        val selection = selection(start = 1, end = 3)

        assertEquals("world\nNext line", selection.selectedText())
        assertEquals(
            listOf(PageRect(12f, 0f, 25f, 10f), PageRect(0f, 15f, 22f, 25f)),
            selection.selectedLineBounds(),
        )
    }

    @Test
    fun draggingHandlePastOtherHandleKeepsAValidRange() {
        val selection = selection(start = 3, end = 0)

        assertEquals("Hello world\nNext line", selection.selectedText())
        assertEquals(PagePoint(22f, 25f), selection.handlePosition(TextSelectionHandle.Start))
        assertEquals(PagePoint(0f, 10f), selection.handlePosition(TextSelectionHandle.End))
    }

    @Test
    fun cjkTokensDoNotGainArtificialSpaces() {
        val cjk = listOf(
            token("日", 0f, 0f, 5f, 10f, line = 0),
            token("本", 5f, 0f, 10f, 10f, line = 0),
            token("語", 10f, 0f, 15f, 10f, line = 0),
        )

        assertEquals("日本語", selection(tokens = cjk, start = 0, end = 2).selectedText())
    }

    @Test
    fun adjacentThaiCharactersDoNotGainSpaces() {
        val thai = listOf(
            token("ภ", 0f, 0f, 5f, 10f, line = 0),
            token("า", 6f, 0f, 11f, 10f, line = 0),
            token("ษา", 12f, 0f, 22f, 10f, line = 0),
        )

        assertEquals("ภาษา", selection(tokens = thai, start = 0, end = 2).selectedText())
    }

    @Test
    fun largeThaiTokenGapPreservesARealSpace() {
        val thai = listOf(
            token("ภาษาไทย", 0f, 0f, 30f, 10f, line = 0),
            token("ทดสอบ", 38f, 0f, 60f, 10f, line = 0),
        )

        assertEquals("ภาษาไทย ทดสอบ", selection(tokens = thai, start = 0, end = 1).selectedText())
    }

    @Test
    fun nearestTokenHonorsActivationDistance() {
        assertEquals(1, nearestRecognizedTokenIndex(tokens, PagePoint(16f, 4f), maxDistance = 2f))
        assertNull(nearestRecognizedTokenIndex(tokens, PagePoint(100f, 100f), maxDistance = 5f))
    }

    @Test
    fun selectAllExpandsBothEndpoints() {
        val selected = selection(start = 1, end = 1).selectAll()

        assertEquals(0, selected.startIndex)
        assertEquals(tokens.lastIndex, selected.endIndex)
    }

    private fun selection(
        tokens: List<RecognizedTextToken> = this.tokens,
        start: Int,
        end: Int,
    ) = TextRangeSelection(PageId("page"), tokens, start, end, "English")

    private fun token(
        text: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        line: Int,
    ) = RecognizedTextToken(text, PageRect(left, top, right, bottom), line)
}
