package com.mheenotes.core.store

import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.Tool
import com.mheenotes.core.model.TextAlign
import com.mheenotes.core.model.FontKind

/** Compact little-endian binary encoding of page items. Version byte first for future changes. */
object ItemCodec {
    /** Version 2 added image lock and text styling, version 3 stroke animation; older items still decode. */
    private const val VERSION: Int = 3
    const val KIND_STROKE = 1
    const val KIND_IMAGE = 2
    const val KIND_TEXT = 3
    const val KIND_SHAPE = 4

    fun kind(item: Item): Int = when (item) {
        is Stroke -> KIND_STROKE
        is ImageItem -> KIND_IMAGE
        is TextItem -> KIND_TEXT
        is ShapeItem -> KIND_SHAPE
    }

    fun encode(item: Item): ByteArray {
        val w = ByteWriter(if (item is Stroke) 16 + item.points.size * 4 else 64)
        w.byte(VERSION)
        when (item) {
            is Stroke -> {
                w.byte(item.tool.ordinal); w.int(item.argb); w.float(item.width)
                w.int(item.points.size)
                for (f in item.points) w.float(f)
                w.int(item.anim)
            }
            is ImageItem -> {
                w.string(item.assetId); w.rect(item.frame); w.float(item.rotation); w.rect(item.crop); w.float(item.aspect)
                w.byte(if (item.locked) 1 else 0)
            }
            is TextItem -> {
                w.string(item.text); w.rect(item.frame); w.float(item.fontSize); w.int(item.argb)
                w.byte(if (item.bold) 1 else 0); w.float(item.rotation)
                w.byte(if (item.italic) 1 else 0); w.byte(if (item.underline) 1 else 0); w.byte(item.align.ordinal); w.byte(item.font.ordinal)
            }
            is ShapeItem -> {
                w.byte(item.kind.ordinal); w.rect(item.frame); w.int(item.argb); w.float(item.strokeWidth)
                w.byte(if (item.fillArgb != null) 1 else 0); w.int(item.fillArgb ?: 0); w.float(item.rotation)
            }
        }
        return w.toByteArray()
    }

    fun decode(id: String, seq: Long, kind: Int, data: ByteArray): Item {
        val r = ByteReader(data)
        val version = r.byte()
        check(version in 1..VERSION) { "Unsupported item version $version" }
        return when (kind) {
            KIND_STROKE -> {
                val tool = Tool.entries[r.byte()]
                val argb = r.int(); val width = r.float()
                val n = r.int()
                val points = FloatArray(n) { r.float() }
                Stroke(id, seq, tool, argb, width, points, if (version >= 3) r.int() else 0)
            }
            KIND_IMAGE -> {
                val img = ImageItem(id, seq, r.string(), r.rect(), r.float(), r.rect(), r.float())
                if (version >= 2) img.copy(locked = r.byte() == 1) else img
            }
            KIND_TEXT -> {
                val t = TextItem(id, seq, r.string(), r.rect(), r.float(), r.int(), r.byte() == 1, r.float())
                if (version >= 2) {
                    t.copy(italic = r.byte() == 1, underline = r.byte() == 1, align = TextAlign.entries[r.byte()], font = FontKind.entries[r.byte()])
                } else t
            }
            KIND_SHAPE -> {
                val k = ShapeKind.entries[r.byte()]
                val frame = r.rect(); val argb = r.int(); val sw = r.float()
                val hasFill = r.byte() == 1; val fill = r.int()
                ShapeItem(id, seq, k, frame, argb, sw, if (hasFill) fill else null, r.float())
            }
            else -> error("Unknown item kind $kind")
        }
    }
}

class ByteWriter(capacity: Int = 64) {
    private var buf = ByteArray(capacity)
    private var pos = 0

    private fun ensure(n: Int) {
        if (pos + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, pos + n))
    }

    fun byte(v: Int) { ensure(1); buf[pos++] = v.toByte() }
    fun int(v: Int) {
        ensure(4)
        buf[pos++] = v.toByte(); buf[pos++] = (v shr 8).toByte()
        buf[pos++] = (v shr 16).toByte(); buf[pos++] = (v shr 24).toByte()
    }
    fun float(v: Float) = int(v.toRawBits())
    fun string(v: String) {
        val b = v.encodeToByteArray()
        int(b.size); ensure(b.size); b.copyInto(buf, pos); pos += b.size
    }
    fun rect(r: Rect) { float(r.left); float(r.top); float(r.right); float(r.bottom) }
    fun toByteArray(): ByteArray = buf.copyOf(pos)
}

class ByteReader(private val buf: ByteArray) {
    private var pos = 0
    fun byte(): Int = buf[pos++].toInt() and 0xff
    fun int(): Int {
        val v = (buf[pos].toInt() and 0xff) or ((buf[pos + 1].toInt() and 0xff) shl 8) or
            ((buf[pos + 2].toInt() and 0xff) shl 16) or ((buf[pos + 3].toInt() and 0xff) shl 24)
        pos += 4
        return v
    }
    fun float(): Float = Float.fromBits(int())
    fun string(): String {
        val n = int()
        val s = buf.decodeToString(pos, pos + n)
        pos += n
        return s
    }
    fun rect() = Rect(float(), float(), float(), float())
}
