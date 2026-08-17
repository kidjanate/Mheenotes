package com.mheenotes.core.model

import java.util.UUID

@JvmInline value class NotebookId(val value: String)
@JvmInline value class ObjectId(val value: String) {
    init { require(value.isNotBlank()) }
}
@JvmInline value class AssetId(val value: String) {
    init {
        require(value.isNotBlank())
        require(".." !in value && '/' !in value && '\\' !in value)
    }
}
@JvmInline value class SourceDocumentId(val value: String)

data class PageSize(val widthMicrons: Float, val heightMicrons: Float) {
    init { require(widthMicrons > 0f && heightMicrons > 0f) }
}

enum class PageTemplate { Blank, Ruled, Grid, Dotted, Cornell, MusicStaff }

data class PdfPageSource(
    val documentId: SourceDocumentId,
    val pageIndex: Int,
) { init { require(pageIndex >= 0) } }

data class ObjectTransform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float = 0f,
) { init { require(width > 0f && height > 0f) } }

sealed interface PageObject {
    val id: ObjectId
    val transform: ObjectTransform
    val zIndex: Long
}

data class ObjectCrop(
    val leftFraction: Float = 0f,
    val topFraction: Float = 0f,
    val rightFraction: Float = 0f,
    val bottomFraction: Float = 0f,
) {
    init {
        require(listOf(leftFraction, topFraction, rightFraction, bottomFraction).all { it in 0f..<1f })
        require(leftFraction + rightFraction < 1f)
        require(topFraction + bottomFraction < 1f)
    }
}

data class ImageObject(
    override val id: ObjectId,
    val assetId: AssetId,
    override val transform: ObjectTransform,
    override val zIndex: Long,
    val opacity: Float = 1f,
    val crop: ObjectCrop = ObjectCrop(),
    val locked: Boolean = false,
) : PageObject

data class TextObject(
    override val id: ObjectId,
    val text: String,
    val argb: Int,
    val textSizeMicrons: Float,
    override val transform: ObjectTransform,
    override val zIndex: Long,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val alignment: TextAlignment = TextAlignment.Start,
    val fontFamily: String = "sans-serif",
    val outlineArgb: Int? = null,
    val outlineWidthMicrons: Float = 0f,
    val curveDegrees: Float = 0f,
) : PageObject

enum class TextAlignment { Start, Center, End }

enum class ShapeKind { Line, Arrow, Rectangle, RoundedRectangle, Ellipse, Triangle }

data class ShapeObject(
    override val id: ObjectId,
    val kind: ShapeKind,
    val strokeArgb: Int,
    val strokeWidthMicrons: Float,
    val fillArgb: Int? = null,
    override val transform: ObjectTransform,
    override val zIndex: Long,
) : PageObject

data class AudioObject(
    override val id: ObjectId,
    val assetId: AssetId,
    val durationMillis: Long,
    val transcript: String = "",
    override val transform: ObjectTransform,
    override val zIndex: Long,
) : PageObject

data class NotebookPage(
    val id: PageId,
    val size: PageSize = PageSize(210_000f, 297_000f),
    val rotationDegrees: Int = 0,
    val template: PageTemplate = PageTemplate.Blank,
    val pdfSource: PdfPageSource? = null,
    val objects: List<PageObject> = emptyList(),
    val bookmarked: Boolean = false,
    val backgroundArgb: Int = 0xFFFFFEFA.toInt(),
    val templateLineArgb: Int = 0x402563EB,
) { init { require(rotationDegrees in setOf(0, 90, 180, 270)) } }

data class SourceDocument(
    val id: SourceDocumentId,
    val assetPath: String,
    val mediaType: String,
    val sha256: String,
    val pageCount: Int,
) { init { require(pageCount > 0); require(".." !in assetPath) } }

data class NotebookDocument(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val id: NotebookId,
    val title: String,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long,
    val pages: List<NotebookPage>,
    val sourceDocuments: List<SourceDocument> = emptyList(),
    val folderPath: String? = null,
    val favorite: Boolean = false,
    val trashedAtEpochMillis: Long? = null,
    val lastOpenedAtEpochMillis: Long = modifiedAtEpochMillis,
    val coverArgb: Int = 0xFF303747.toInt(),
) {
    init {
        require(formatVersion in 1..CURRENT_FORMAT_VERSION)
        require(title.isNotBlank())
        require(pages.isNotEmpty())
        require(pages.map { it.id }.distinct().size == pages.size)
    }

    companion object {
        const val CURRENT_FORMAT_VERSION = 6
        fun empty(title: String = "Untitled", now: Long = System.currentTimeMillis()) = NotebookDocument(
            id = NotebookId(UUID.randomUUID().toString()),
            title = title,
            createdAtEpochMillis = now,
            modifiedAtEpochMillis = now,
            pages = listOf(NotebookPage(PageId(UUID.randomUUID().toString()))),
        )
    }
}

class NotebookEditor(initial: NotebookDocument) {
    var document: NotebookDocument = initial
        private set

    fun insertPage(index: Int, page: NotebookPage = NotebookPage(PageId(UUID.randomUUID().toString()))) {
        require(index in 0..document.pages.size)
        document = document.copy(pages = document.pages.toMutableList().apply { add(index, page) })
    }

    fun deletePage(pageId: PageId) {
        require(document.pages.size > 1) { "A notebook must keep at least one page" }
        document = document.copy(pages = document.pages.filterNot { it.id == pageId })
    }

    fun duplicatePage(pageId: PageId) {
        val index = document.pages.indexOfFirst { it.id == pageId }
        require(index >= 0)
        val copy = document.pages[index].copy(id = PageId(UUID.randomUUID().toString()))
        insertPage(index + 1, copy)
    }

    fun movePage(from: Int, to: Int) {
        require(from in document.pages.indices && to in document.pages.indices)
        val pages = document.pages.toMutableList()
        pages.add(to, pages.removeAt(from))
        document = document.copy(pages = pages)
    }

    fun rotatePage(pageId: PageId) = updatePage(pageId) {
        it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360)
    }

    fun toggleBookmark(pageId: PageId) = updatePage(pageId) { it.copy(bookmarked = !it.bookmarked) }

    fun addObject(pageId: PageId, value: PageObject) = updatePage(pageId) {
        it.copy(objects = (it.objects + value).sortedBy(PageObject::zIndex))
    }

    fun replaceObject(pageId: PageId, value: PageObject) = updatePage(pageId) {
        require(it.objects.any { item -> item.id == value.id })
        it.copy(objects = it.objects.map { item -> if (item.id == value.id) value else item })
    }

    fun removeObjects(pageId: PageId, ids: Set<ObjectId>) = updatePage(pageId) {
        it.copy(objects = it.objects.filterNot { item -> item.id in ids })
    }

    private fun updatePage(pageId: PageId, operation: (NotebookPage) -> NotebookPage) {
        require(document.pages.any { it.id == pageId })
        document = document.copy(pages = document.pages.map { if (it.id == pageId) operation(it) else it })
    }
}
