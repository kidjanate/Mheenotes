package com.mheenotes.app.ui.editor

import com.mheenotes.core.model.PageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePreviewTest {
    private val portrait = PageSize(210_000f, 297_000f)

    @Test
    fun quarterTurnUsesRotatedPaperAspectRatio() {
        assertEquals(297f / 210f, pagePreviewAspectRatio(portrait, 90), .0001f)
        assertEquals(297f / 210f, pagePreviewAspectRatio(portrait, 270), .0001f)
    }

    @Test
    fun halfTurnKeepsOriginalPaperAspectRatio() {
        assertEquals(210f / 297f, pagePreviewAspectRatio(portrait, 0), .0001f)
        assertEquals(210f / 297f, pagePreviewAspectRatio(portrait, 180), .0001f)
    }

    @Test
    fun pageSnapshotReplacementKeepsAValidPageList() {
        val pages = mutableListOf("first", "new page")

        pages.replaceContentsWithoutEmptying(listOf("first"))

        assertTrue(pages.isNotEmpty())
        assertEquals(listOf("first"), pages)
    }
}
