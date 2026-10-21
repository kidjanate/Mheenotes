package com.mheenotes.core

import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.doc.PageContent
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.geometry.StrokeGeometry
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.newId
import com.mheenotes.core.store.AppFiles
import com.mheenotes.core.store.ItemCodec
import com.mheenotes.core.store.Library
import com.mheenotes.core.store.NotebookStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun line(x0: Float, y0: Float, x1: Float, y1: Float, n: Int = 20, seq: Long = 1, tool: Tool = Tool.Pen) =
    Stroke(newId(), seq, tool, 0xFF000000.toInt(), 2f, FloatArray(n * 3) { i ->
        val k = i / 3; val t = k / (n - 1f)
        when (i % 3) { 0 -> x0 + (x1 - x0) * t; 1 -> y0 + (y1 - y0) * t; else -> 0.5f }
    })

class CoreTest {
    @Test
    fun gridQueryFindsOnlyNearbyItemsInDrawOrder() {
        val a = line(0f, 0f, 50f, 0f, seq = 2)
        val b = line(500f, 500f, 550f, 500f, seq = 1)
        val hl = line(0f, 10f, 50f, 10f, seq = 3, tool = Tool.Highlighter)
        val c = PageContent(listOf(a, b, hl))
        val near = c.snapshot.query(Rect(-5f, -5f, 60f, 20f))
        assertEquals(listOf(hl.id, a.id), near.map { it.id }, "highlighter draws first")
        c.apply(removed = listOf(a), added = emptyList())
        assertEquals(listOf(hl.id), c.snapshot.query(Rect(-5f, -5f, 60f, 20f)).map { it.id })
        assertEquals(2, c.snapshot.items.size)
    }

    @Test
    fun appendingToDensePageKeepsOrderAndIsFast() {
        val c = PageContent((1..20_000).map { line((it % 500).toFloat(), (it / 25).toFloat(), (it % 500) + 5f, (it / 25).toFloat(), n = 8, seq = it.toLong()) })
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(200) { c.apply(emptyList(), listOf(line(10f, 10f, 20f, 20f, seq = 30_000L + it))) }
        val perEdit = start.elapsedNow() / 200
        assertTrue(perEdit.inWholeMilliseconds < 5, "append took $perEdit")
        assertEquals(20_200, c.snapshot.items.size)
        assertTrue(c.snapshot.items.zipWithNext().all { (x, y) -> x.seq < y.seq })
    }

    @Test
    fun partialEraseSplitsStroke() {
        val s = line(0f, 0f, 100f, 0f, n = 101)
        var seq = 10L
        val pieces = assertNotNull(StrokeGeometry.eraseCircle(s, 50f, 0f, 5f) { ++seq })
        assertEquals(2, pieces.size)
        assertTrue(pieces[0].points[pieces[0].points.size - 3] < 47f)
        assertTrue(pieces[1].points[0] > 53f)
        assertNull(StrokeGeometry.eraseCircle(s, 50f, 40f, 5f) { ++seq })
    }

    @Test
    fun codecRoundTrips() {
        val s = line(1f, 2f, 3f, 4f, n = 5)
        val back = ItemCodec.decode(s.id, s.seq, ItemCodec.KIND_STROKE, ItemCodec.encode(s)) as Stroke
        assertTrue(back.points.contentEquals(s.points))
        val shape = ShapeItem(newId(), 3, ShapeKind.Ellipse, Rect(1f, 2f, 30f, 40f), 7, 2f, 0x55000000, 15f)
        assertEquals(shape, ItemCodec.decode(shape.id, 3, ItemCodec.KIND_SHAPE, ItemCodec.encode(shape)))
    }

    @Test
    fun documentPersistsEditsAndUndo() = runTest {
        val dir = kotlin.io.path.createTempDirectory("mheenotes").toString()
        val files = AppFiles(dir)
        val id = newId()
        val page = Page(newId(), 1.0, 595f, 842f)
        NotebookStore(id, files).apply { writePages(listOf(page), emptyList()); flush(); close() }

        val doc = NotebookDocument.open(NotebookStore(id, files), this)
        doc.load(page.id)
        val s1 = line(0f, 0f, 10f, 10f, seq = doc.nextSeq())
        val s2 = line(5f, 5f, 20f, 20f, seq = doc.nextSeq())
        doc.edit(page.id, emptyList(), listOf(s1))
        doc.edit(page.id, emptyList(), listOf(s2))
        doc.undo()
        assertEquals(1, doc.content(page.id)!!.snapshot.items.size)
        doc.close()

        val reopened = NotebookDocument.open(NotebookStore(id, files), this)
        assertEquals(listOf(s1.id), reopened.load(page.id).snapshot.items.map { it.id })
        assertEquals(1, reopened.pages.value.size)
        reopened.close()

        val lib = Library(files)
        lib.load()
        lib.upsert(Notebook(id, "Physics", null, 1, 1, 1, 1))
        lib.flush()
        val lib2 = Library(files).apply { load() }
        assertEquals("Physics", lib2.state.value.notebooks.single().title)
    }
}

class RecognizerTest {
    private fun pts(vararg xy: Float): FloatArray {
        // Densify polyline so recognizers see realistic sampling.
        val out = ArrayList<Float>()
        for (i in 0 until xy.size / 2 - 1) {
            val x0 = xy[i * 2]; val y0 = xy[i * 2 + 1]; val x1 = xy[i * 2 + 2]; val y1 = xy[i * 2 + 3]
            for (k in 0 until 10) { val t = k / 10f; out += x0 + (x1 - x0) * t; out += y0 + (y1 - y0) * t; out += 0.5f }
        }
        out += xy[xy.size - 2]; out += xy[xy.size - 1]; out += 0.5f
        return out.toFloatArray()
    }

    @Test
    fun recognizesBasicShapes() {
        val sr = com.mheenotes.core.geometry.ShapeRecognizer
        assertEquals(com.mheenotes.core.model.ShapeKind.Line, sr.recognize(pts(0f, 0f, 100f, 3f))?.kind)
        assertEquals(com.mheenotes.core.model.ShapeKind.Rectangle, sr.recognize(pts(0f, 0f, 100f, 0f, 100f, 60f, 0f, 60f, 0f, 1f))?.kind)
        assertEquals(com.mheenotes.core.model.ShapeKind.Triangle, sr.recognize(pts(50f, 0f, 100f, 80f, 0f, 80f, 49f, 1f))?.kind)
        val circle = FloatArray(60 * 3) { i -> val k = i / 3; val a = k / 59f * 6.2832f; when (i % 3) { 0 -> 50 + 40 * kotlin.math.cos(a); 1 -> 50 + 40 * kotlin.math.sin(a); else -> 0.5f } }
        assertEquals(com.mheenotes.core.model.ShapeKind.Ellipse, sr.recognize(circle)?.kind)
    }

    @Test
    fun scribbleErasesCoveredStroke() {
        val word = line(10f, 50f, 60f, 50f)
        val zigzag = pts(5f, 45f, 65f, 55f, 5f, 46f, 65f, 54f, 5f, 47f, 65f, 53f, 5f, 48f, 65f, 52f)
        assertTrue(com.mheenotes.core.geometry.ScribbleDetector.isScribble(zigzag))
        assertEquals(listOf(word.id), com.mheenotes.core.geometry.ScribbleDetector.victims(zigzag, 2f, listOf(word, line(200f, 200f, 300f, 200f))).map { it.id })
        assertTrue(!com.mheenotes.core.geometry.ScribbleDetector.isScribble(pts(0f, 0f, 100f, 0f)))
    }

    @Test
    fun lassoSelectsMostlyInsideStrokes() {
        val lasso = com.mheenotes.core.geometry.Lasso(floatArrayOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f))
        assertTrue(lasso.selects(line(10f, 10f, 90f, 90f)))
        assertTrue(!lasso.selects(line(80f, 50f, 300f, 50f)))
    }
}
