@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.mheenotes.core.model

import com.mheenotes.core.geometry.Rect
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Stable, globally unique identifiers keep every record mergeable when sync arrives later. */
typealias Id = String

fun newId(): Id = Uuid.random().toHexString()

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

data class Folder(
    val id: Id,
    val name: String,
    val parentId: Id?,
    val createdAt: Long,
    val trashedAt: Long? = null,
)

data class Notebook(
    val id: Id,
    val title: String,
    val folderId: Id?,
    val coverArgb: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val openedAt: Long,
    val favorite: Boolean = false,
    val trashedAt: Long? = null,
    val pageCount: Int = 1,
    /** Bumped whenever the first page changes so cached covers can be invalidated cheaply. */
    val thumbVersion: Long = 0,
)

enum class Template { Blank, Lined, Grid, Dotted }

/** Page units are PDF points (1/72 inch), so imported PDFs map 1:1. */
data class PageSize(val width: Float, val height: Float) {
    companion object {
        val A4 = PageSize(595f, 842f)
        val A5 = PageSize(420f, 595f)
        val Letter = PageSize(612f, 792f)
        val Square = PageSize(700f, 700f)
    }
}

data class PdfRef(val assetId: Id, val pageIndex: Int)

data class Page(
    val id: Id,
    /** Fractional order key: inserting between two pages never renumbers the others. */
    val ord: Double,
    val width: Float,
    val height: Float,
    val template: Template = Template.Blank,
    val backgroundArgb: Int = PAPER_WHITE,
    val pdf: PdfRef? = null,
)

const val PAPER_WHITE: Int = 0xFFFFFFFF.toInt()

enum class Tool { Pen, Pencil, Highlighter }

enum class ShapeKind { Line, Arrow, Rectangle, Ellipse, Triangle }

/** Everything placed on a page. Items are immutable; edits replace them. */
sealed interface Item {
    val id: Id
    /** Z-order. Larger draws later. */
    val seq: Long
    val bounds: Rect
}

/**
 * [points] packs x, y, pressure triples in page units. Width is the nominal diameter at full pressure.
 */
class Stroke(
    override val id: Id,
    override val seq: Long,
    val tool: Tool,
    val argb: Int,
    val width: Float,
    val points: FloatArray,
) : Item {
    val pointCount: Int get() = points.size / 3
    override val bounds: Rect by lazy { com.mheenotes.core.geometry.StrokeGeometry.bounds(this) }

    fun copy(
        id: Id = this.id,
        seq: Long = this.seq,
        argb: Int = this.argb,
        width: Float = this.width,
        points: FloatArray = this.points,
    ) = Stroke(id, seq, tool, argb, width, points)
}

data class ImageItem(
    override val id: Id,
    override val seq: Long,
    val assetId: Id,
    /** Unrotated frame in page units. */
    val frame: Rect,
    val rotation: Float = 0f,
    /** Normalized crop window of the source image. */
    val crop: Rect = Rect(0f, 0f, 1f, 1f),
    val aspect: Float,
) : Item {
    override val bounds: Rect get() = frame.rotatedBounds(rotation)
}

data class TextItem(
    override val id: Id,
    override val seq: Long,
    val text: String,
    /** Width is fixed by the user; height follows the laid-out text. */
    val frame: Rect,
    val fontSize: Float,
    val argb: Int,
    val bold: Boolean = false,
    val rotation: Float = 0f,
) : Item {
    override val bounds: Rect get() = frame.rotatedBounds(rotation)
}

data class ShapeItem(
    override val id: Id,
    override val seq: Long,
    val kind: ShapeKind,
    /** Line and arrow run from (left, top) to (right, bottom); other kinds fill the box. */
    val frame: Rect,
    val argb: Int,
    val strokeWidth: Float,
    val fillArgb: Int? = null,
    val rotation: Float = 0f,
) : Item {
    override val bounds: Rect get() = when (kind) {
        ShapeKind.Line, ShapeKind.Arrow -> frame.normalized().outset(strokeWidth * 3)
        else -> frame.normalized().rotatedBounds(rotation).outset(strokeWidth)
    }
}

data class Asset(val id: Id, val mime: String, val fileName: String)
