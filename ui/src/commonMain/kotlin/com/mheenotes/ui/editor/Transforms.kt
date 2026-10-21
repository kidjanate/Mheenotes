package com.mheenotes.ui.editor

import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.geometry.Similarity
import com.mheenotes.core.geometry.StrokeGeometry
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.TextItem

/** Applies a move/scale/rotate to any item. Boxes keep their own rotation field. */
fun transformItem(item: Item, t: Similarity): Item {
    if (t.isIdentity) return item
    return when (item) {
        is Stroke -> StrokeGeometry.transform(item, t)
        is ShapeItem -> when (item.kind) {
            ShapeKind.Line, ShapeKind.Arrow -> item.copy(
                frame = Rect(
                    t.mapX(item.frame.left, item.frame.top), t.mapY(item.frame.left, item.frame.top),
                    t.mapX(item.frame.right, item.frame.bottom), t.mapY(item.frame.right, item.frame.bottom),
                ),
                strokeWidth = item.strokeWidth * t.scale,
            )
            else -> item.copy(frame = mapFrame(item.frame.normalized(), t), rotation = item.rotation + t.rotation, strokeWidth = item.strokeWidth * t.scale)
        }
        is TextItem -> item.copy(frame = mapFrame(item.frame, t), rotation = item.rotation + t.rotation, fontSize = item.fontSize * t.scale)
        is ImageItem -> item.copy(frame = mapFrame(item.frame, t), rotation = item.rotation + t.rotation)
    }
}

/** Moves the frame's center through [t] and scales its size. */
private fun mapFrame(f: Rect, t: Similarity): Rect {
    val cx = t.mapX(f.centerX, f.centerY)
    val cy = t.mapY(f.centerX, f.centerY)
    val hw = f.width * t.scale / 2
    val hh = f.height * t.scale / 2
    return Rect(cx - hw, cy - hh, cx + hw, cy + hh)
}
