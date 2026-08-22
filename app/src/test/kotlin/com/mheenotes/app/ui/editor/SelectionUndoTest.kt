package com.mheenotes.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionUndoTest {
    @Test
    fun selectionWithoutAnEditOnlyCancelsSelection() {
        assertEquals(
            SelectionUndoAction.CancelSelection,
            selectionUndoAction(baselineRevision = 7L, currentRevision = 7L),
        )
    }

    @Test
    fun selectionEditIsUndoneAndThenSelectionIsCancelled() {
        assertEquals(
            SelectionUndoAction.UndoEditAndCancelSelection,
            selectionUndoAction(baselineRevision = 7L, currentRevision = 8L),
        )
    }

    @Test
    fun ordinaryUndoHasNoSelectionSideEffect() {
        assertEquals(
            SelectionUndoAction.None,
            selectionUndoAction(baselineRevision = null, currentRevision = 8L),
        )
    }
}
