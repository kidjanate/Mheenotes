package com.mheenotes.ui.platform

import android.net.Uri
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PdfRef
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.newId
import com.mheenotes.core.model.nowMillis
import com.mheenotes.core.store.NotebookStore
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.util.Properties
import java.util.zip.CRC32
import java.util.zip.ZipInputStream
import kotlin.math.pow

/**
 * One-way import of `.mynote` files from the legacy app: a zip with `manifest.properties`,
 * `pages/<pageId>.journal` (MHJ1 stroke journals) and `assets/`. Lengths are microns there and
 * points here. Audio notes are skipped (not in release 1).
 */
object LegacyImport {
    private const val UM_TO_PT = 72f / 25_400f

    suspend fun import(uri: Uri, folderId: Id?): Notebook =
        importStream({ AndroidContext.app.contentResolver.openInputStream(uri)!! }, folderId)

    suspend fun importFile(file: File, folderId: Id?): Notebook = importStream({ file.inputStream() }, folderId)

    private suspend fun importStream(open: () -> java.io.InputStream, folderId: Id?): Notebook = withContext(Dispatchers.IO) {
        val work = File(AppGraph.files.tempDir, "legacy-${newId()}").apply { mkdirs() }
        try {
            unzip(open(), work)
            convert(work, folderId)
        } finally {
            work.deleteRecursively()
        }
    }

    private fun unzip(input: java.io.InputStream, dir: File) {
        val root = dir.canonicalPath + File.separator
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val out = File(dir, e.name).canonicalFile
                require(out.path.startsWith(root)) { "Bad archive entry" }
                if (e.isDirectory) { out.mkdirs(); continue }
                out.parentFile?.mkdirs()
                out.outputStream().use { zip.copyTo(it) }
            }
        }
    }

    private suspend fun convert(dir: File, folderId: Id?): Notebook {
        val manifest = File(dir, "manifest.properties").takeIf { it.isFile } ?: File(dir, "manifest.properties.backup")
        val p = Properties().apply { manifest.inputStream().use { load(it) } }
        fun req(k: String) = p.getProperty(k) ?: error("Missing $k")

        val id = newId()
        val assetsDir = File(AppGraph.files.assetsDir(id)).apply { mkdirs() }
        // PDF sources become our PDF assets.
        val sources = HashMap<String, Id>()
        val sourceSizes = HashMap<String, List<Pair<Float, Float>>>()
        for (i in 0 until (p.getProperty("sourceCount")?.toInt() ?: 0)) {
            val srcId = req("source.$i.id")
            val file = File(dir, req("source.$i.assetPath"))
            if (!file.isFile) continue
            val assetId = newId()
            file.copyTo(File(assetsDir, "$assetId.pdf"))
            sources[srcId] = assetId
            sourceSizes[srcId] = runCatching { com.mheenotes.ui.editor.PdfPages.sizes(file) }.getOrDefault(emptyList())
        }
        File(dir, "assets").listFiles()?.filter { it.name.endsWith(".image") }?.forEach { it.copyTo(File(assetsDir, it.name), true) }

        val pages = ArrayList<Page>()
        val items = HashMap<Id, List<Item>>()
        val pageCount = req("pageCount").toInt()
        for (i in 0 until pageCount) {
            val k = "page.$i."
            val legacyId = req(k + "id")
            // Legacy PDF pages were sized in points x 1000 rather than microns, so scale those
            // pages to the PDF's real size; everything on the page scales with them.
            val legacyW = req(k + "width").toFloat()
            val pdfSize = p.getProperty(k + "pdfSourceId")?.let { s -> sourceSizes[s]?.getOrNull(req(k + "pdfPageIndex").toInt()) }
            val u = if (pdfSize != null) pdfSize.first / legacyW else UM_TO_PT
            val w = legacyW * u
            val h = req(k + "height").toFloat() * u
            val template = when (req(k + "template")) {
                "Blank" -> Template.Blank
                "Grid" -> Template.Grid
                "Dotted" -> Template.Dotted
                else -> Template.Lined
            }
            val pdf = p.getProperty(k + "pdfSourceId")?.let { s -> sources[s]?.let { PdfRef(it, req(k + "pdfPageIndex").toInt()) } }
            val bg = p.getProperty(k + "backgroundArgb")?.toInt() ?: 0xFFFFFFFF.toInt()
            val page = Page(newId(), (i + 1).toDouble(), w, h, if (pdf != null) Template.Blank else template, bg, pdf)
            pages += page

            val list = ArrayList<Item>()
            for (o in 0 until (p.getProperty(k + "objectCount")?.toInt() ?: 0)) {
                readObject(p, "${k}object.$o.", u)?.let { list += it }
            }
            val journal = File(dir, "pages/$legacyId.journal")
            if (journal.isFile) list += readJournal(journal, legacyId, u)
            items[page.id] = list
        }
        require(pages.isNotEmpty()) { "Notebook has no pages" }

        val title = p.getProperty("title")?.ifBlank { null } ?: "Imported"
        val store = NotebookStore(id, AppGraph.files)
        store.putMeta("title", title)
        store.writeBulk(pages, items)
        store.close()
        val now = nowMillis()
        return Notebook(
            id, title, folderId, 0,
            createdAt = p.getProperty("createdAt")?.toLongOrNull() ?: now,
            updatedAt = p.getProperty("modifiedAt")?.toLongOrNull() ?: now,
            openedAt = now,
            favorite = p.getProperty("favorite") == "true",
            pageCount = pages.size,
        )
    }

    private fun readObject(p: Properties, k: String, u: Float): Item? {
        val x = p.getProperty(k + "x")?.toFloat()?.times(u) ?: return null
        val y = p.getProperty(k + "y")!!.toFloat() * u
        val w = p.getProperty(k + "width")!!.toFloat() * u
        val h = p.getProperty(k + "height")!!.toFloat() * u
        val rot = p.getProperty(k + "rotation")?.toFloat() ?: 0f
        val seq = p.getProperty(k + "zIndex")?.toLong() ?: 0L
        val frame = Rect(x, y, x + w, y + h)
        return when (p.getProperty(k + "type")) {
            "image" -> {
                fun f(n: String) = p.getProperty(k + n)?.toFloat() ?: 0f
                ImageItem(newId(), seq, p.getProperty(k + "assetId")!!, frame, rot, Rect(f("cropLeft"), f("cropTop"), 1f - f("cropRight"), 1f - f("cropBottom")), w / h)
            }
            "text" -> TextItem(
                newId(), seq, p.getProperty(k + "text") ?: "", frame,
                (p.getProperty(k + "textSize")?.toFloat() ?: 4_500f) * u,
                p.getProperty(k + "argb")?.toInt() ?: 0xFF000000.toInt(),
                p.getProperty(k + "bold") == "true", rot,
            )
            "shape" -> {
                val kind = when (p.getProperty(k + "kind")) {
                    "Line" -> ShapeKind.Line
                    "Arrow" -> ShapeKind.Arrow
                    "Ellipse" -> ShapeKind.Ellipse
                    "Triangle" -> ShapeKind.Triangle
                    else -> ShapeKind.Rectangle
                }
                ShapeItem(
                    newId(), seq, kind, frame, p.getProperty(k + "strokeArgb")?.toInt() ?: 0xFF000000.toInt(),
                    (p.getProperty(k + "strokeWidth")?.toFloat() ?: 600f) * u,
                    p.getProperty(k + "fillArgb")?.toInt(), rot,
                )
            }
            else -> null // audio notes are not part of release 1
        }
    }

    /** Replays an MHJ1 journal (add/delete/replace records) into its final stroke set. */
    private fun readJournal(file: File, pageId: String, u: Float): List<Item> {
        val strokes = LinkedHashMap<String, Stroke>()
        DataInputStream(file.inputStream().buffered()).use { input ->
            val magic = ByteArray(4); input.readFully(magic)
            require(magic.contentEquals(byteArrayOf(0x4D, 0x48, 0x4A, 0x31))) { "Not a journal" }
            require(input.readInt() == 1)
            require(input.readUTF() == pageId)
            while (true) {
                val size = runCatching { input.readInt() }.getOrNull() ?: break
                if (size !in 1..32 * 1024 * 1024) break
                val payload = ByteArray(size)
                if (runCatching { input.readFully(payload) }.isFailure) break
                val crc = runCatching { input.readInt().toLong() and 0xFFFFFFFFL }.getOrNull() ?: break
                if (CRC32().apply { update(payload) }.value != crc) break // torn tail: keep what was committed
                applyRecord(payload, strokes, u)
            }
        }
        return strokes.values.toList()
    }

    private fun applyRecord(payload: ByteArray, strokes: MutableMap<String, Stroke>, u: Float) {
        DataInputStream(ByteArrayInputStream(payload)).use { r ->
            r.readLong()
            when (r.readUnsignedByte()) {
                1 -> readStroke(r, u)?.let { strokes[it.first] = it.second }
                2 -> strokes.remove(r.readUTF())
                3 -> {
                    strokes.remove(r.readUTF())
                    repeat(r.readInt()) { readStroke(r, u)?.let { strokes[it.first] = it.second } }
                }
                4 -> repeat(r.readInt()) { strokes.remove(r.readUTF()) }
                5 -> repeat(r.readInt()) { readStroke(r, u)?.let { strokes[it.first] = it.second } }
                6 -> {
                    repeat(r.readInt()) { strokes.remove(r.readUTF()) }
                    repeat(r.readInt()) { readStroke(r, u)?.let { strokes[it.first] = it.second } }
                }
            }
        }
    }

    private fun DataInputStream.optFloat(): Float? = if (readBoolean()) readFloat() else null

    /** Returns (legacy id, converted stroke); eraser-tool strokes are dropped. */
    private fun readStroke(r: DataInputStream, u: Float): Pair<String, Stroke>? {
        val id = r.readUTF(); r.readUTF()
        val z = r.readLong()
        val toolOrdinal = r.readInt()
        val argb = r.readInt()
        val minW = r.readFloat(); val maxW = r.readFloat()
        val opacity = r.readFloat()
        val pressureEnabled = r.readBoolean()
        val minP = r.readFloat(); val maxP = r.readFloat(); val exp = r.readFloat()
        val n = r.readInt()
        val tool = when (toolOrdinal) { 0 -> Tool.Pen; 1 -> Tool.Pencil; 2 -> Tool.Highlighter; else -> null }
        val pts = FloatArray(n * 3)
        for (i in 0 until n) {
            pts[i * 3] = r.readFloat() * u
            pts[i * 3 + 1] = r.readFloat() * u
            r.readLong()
            val raw = r.optFloat()
            r.optFloat(); r.optFloat()
            // Reproduce the legacy width, then invert our own pressure curve to match it.
            val t = if (!pressureEnabled || raw == null) 0.5f else (((raw - minP) / (maxP - minP)).coerceIn(0f, 1f)).pow(exp)
            val legacyWidth = minW + (maxW - minW) * t
            pts[i * 3 + 2] = invertPressure(tool, legacyWidth / maxW)
        }
        tool ?: return null
        val alpha = ((argb ushr 24) * opacity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        val color = (alpha shl 24) or (argb and 0x00FFFFFF)
        return id to Stroke(newId(), z, tool, color, maxW * u, pts)
    }

    private fun invertPressure(tool: Tool?, ratio: Float): Float = when (tool) {
        Tool.Pen -> ((ratio - 0.28f) / 0.72f).coerceIn(0f, 1f).pow(1f / 0.8f)
        Tool.Pencil -> ((ratio - 0.55f) / 0.45f).coerceIn(0f, 1f)
        else -> 1f
    }
}
