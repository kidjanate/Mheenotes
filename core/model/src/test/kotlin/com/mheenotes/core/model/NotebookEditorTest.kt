package com.mheenotes.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookEditorTest {
    @Test fun pageEditingPreservesInvariants() {
        val editor = NotebookEditor(NotebookDocument.empty(now = 1))
        val first = editor.document.pages.single().id
        editor.duplicatePage(first)
        assertEquals(2, editor.document.pages.size)
        editor.rotatePage(first)
        editor.toggleBookmark(first)
        assertEquals(90, editor.document.pages.first { it.id == first }.rotationDegrees)
        assertTrue(editor.document.pages.first { it.id == first }.bookmarked)
        editor.deletePage(first)
        assertEquals(1, editor.document.pages.size)
    }
}
