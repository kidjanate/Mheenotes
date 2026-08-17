package com.mheenotes.core.storage

import com.mheenotes.core.commands.AddStroke
import com.mheenotes.core.commands.DeleteStroke
import com.mheenotes.core.commands.AddStrokes
import com.mheenotes.core.commands.ReplaceStrokes
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageJournalTest {
    private val pageId = PageId("page")

    @Test
    fun replaysAddAndDeleteOperations() {
        withJournal { journal ->
            val stroke = stroke("one")
            journal.append(AddStroke(stroke))
            journal.append(DeleteStroke(stroke.id))

            val loaded = PageJournal(journalFile, pageId).load()

            assertTrue(loaded.page.strokes.isEmpty())
            assertEquals(2L, loaded.operationCount)
            assertFalse(loaded.recoveredTail)
        }
    }

    @Test
    fun ignoresTornFinalRecordAndKeepsEarlierData() {
        withJournal { journal ->
            val first = stroke("one")
            journal.append(AddStroke(first))
            journal.append(AddStroke(stroke("two")))
            val validLength = journalFile.length()
            journalFile.setLengthForTest(validLength - 7)

            val loaded = PageJournal(journalFile, pageId).load()

            assertEquals(listOf(first), loaded.page.strokes)
            assertEquals(1L, loaded.operationCount)
            assertTrue(loaded.recoveredTail)
        }
    }

    @Test
    fun detectsChecksumCorruption() {
        withJournal { journal ->
            journal.append(AddStroke(stroke("one")))
            val bytes = journalFile.readBytes()
            bytes[bytes.lastIndex - 5] = (bytes[bytes.lastIndex - 5].toInt() xor 0x20).toByte()
            FileOutputStream(journalFile, false).use { it.write(bytes) }

            val loaded = PageJournal(journalFile, pageId).load()

            assertTrue(loaded.page.strokes.isEmpty())
            assertTrue(loaded.recoveredTail)
        }
    }

    @Test
    fun canAppendAfterRecoveringTornTail() {
        withJournal { journal ->
            val first = stroke("one")
            journal.append(AddStroke(first))
            journal.append(AddStroke(stroke("torn")))
            journalFile.setLengthForTest(journalFile.length() - 5)

            val reopened = PageJournal(journalFile, pageId)
            assertTrue(reopened.load().recoveredTail)
            val second = stroke("two")
            reopened.append(AddStroke(second))

            val finalLoad = PageJournal(journalFile, pageId).load()
            assertEquals(listOf(first, second), finalLoad.page.strokes)
            assertFalse(finalLoad.recoveredTail)
            assertTrue(journalFile.parentFile.listFiles().orEmpty().any {
                it.name.contains("recovered-tail")
            })
        }
    }

    @Test
    fun replaysAtomicSelectionPasteAndTransform() {
        withJournal { journal ->
            val first = stroke("one")
            val second = stroke("two")
            val moved = stroke("moved")
            journal.append(AddStrokes(listOf(first, second)))
            journal.append(ReplaceStrokes(setOf(first.id, second.id), listOf(moved)))
            assertEquals(listOf(moved), PageJournal(journalFile, pageId).load().page.strokes)
        }
    }

    private lateinit var journalFile: File

    private fun withJournal(block: (PageJournal) -> Unit) {
        val directory = Files.createTempDirectory("mheenotes-journal-test").toFile()
        try {
            journalFile = File(directory, "page.journal")
            block(PageJournal(journalFile, pageId))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun stroke(id: String): InkStroke {
        val samples = listOf(
            StrokeSample(PagePoint(10f, 20f), 0, 0.3f, null, null),
            StrokeSample(PagePoint(30f, 40f), 1_000, 0.7f, 0.2f, 0.1f),
        )
        return InkStroke(
            id = StrokeId(id),
            pageId = pageId,
            style = StrokeStyle(),
            samples = samples,
            bounds = PageRect.enclosing(samples),
            zIndex = 0,
        )
    }

    private fun File.setLengthForTest(length: Long) {
        java.io.RandomAccessFile(this, "rw").use { it.setLength(length) }
    }
}
