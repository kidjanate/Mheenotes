package com.mheenotes.ui.dev

import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.newId
import com.mheenotes.core.model.nowMillis
import com.mheenotes.core.store.NotebookStore
import com.mheenotes.ui.AppGraph
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** Synthetic data for the performance targets (1,000 notebooks, 20,000 strokes on a page). */
object Fixtures {
    var enabled = false

    fun library(count: Int) {
        val now = nowMillis()
        val rnd = Random(7)
        val words = listOf("Physics", "Calculus", "Meeting", "Ideas", "Journal", "Thai literature", "Chemistry", "Sketches", "Lecture", "Project")
        val list = List(count) { i ->
            Notebook(
                newId(), "${words[i % words.size]} ${i + 1}", null, 0,
                now - rnd.nextLong(0, 90L * 86_400_000L), now - rnd.nextLong(0, 30L * 86_400_000L), now - i * 60_000L,
                favorite = i % 17 == 0, pageCount = 1 + rnd.nextInt(40),
            )
        }
        AppGraph.io.launch {
            for (n in list) {
                val store = NotebookStore(n.id, AppGraph.files)
                store.writePages(listOf(Page(newId(), 1.0, PageSize.A4.width, PageSize.A4.height, Template.entries[rnd.nextInt(4)])), emptyList())
                store.close()
            }
        }
        AppGraph.library.upsertAll(list)
    }

    fun densePage(strokes: Int): Notebook {
        val n = AppGraph.createNotebook("Dense ${strokes / 1000}k", null, Template.Lined, PageSize.A4)
        AppGraph.io.launch {
            val store = NotebookStore(n.id, AppGraph.files)
            val page = store.pages().first()
            val rnd = Random(3)
            val perRow = 40
            val rows = (strokes + perRow - 1) / perRow
            val rowH = (page.height - 60f) / rows
            val items = List(strokes) { i ->
                val row = i / perRow
                val col = i % perRow
                val x0 = 30f + col * ((page.width - 60f) / perRow)
                val y0 = 40f + row * rowH
                val pts = FloatArray(24 * 3)
                for (k in 0 until 24) {
                    val t = k / 23f
                    pts[k * 3] = x0 + t * 11f
                    pts[k * 3 + 1] = y0 + sin(t * 9f + i) * rowH * 0.35f + rnd.nextFloat() * 0.4f
                    pts[k * 3 + 2] = 0.4f + 0.5f * sin(t * 3.1f)
                }
                Stroke(newId(), i + 1L, Tool.Pen, 0xFF1C2541.toInt(), 1.4f, pts)
            }
            store.writeBulk(emptyList(), mapOf(page.id to items))
            store.close()
        }
        return n
    }
}
