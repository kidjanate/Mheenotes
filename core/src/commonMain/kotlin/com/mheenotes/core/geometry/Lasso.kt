package com.mheenotes.core.geometry

import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Stroke

/** Closed lasso polygon in page units, packed as x, y pairs. */
class Lasso(private val xy: FloatArray) {
    val bounds: Rect = run {
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        var i = 0
        while (i < xy.size) {
            l = minOf(l, xy[i]); r = maxOf(r, xy[i]); t = minOf(t, xy[i + 1]); b = maxOf(b, xy[i + 1]); i += 2
        }
        Rect(l, t, r, b)
    }

    fun contains(x: Float, y: Float): Boolean {
        if (!bounds.contains(x, y)) return false
        var inside = false
        val n = xy.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val xi = xy[i * 2]; val yi = xy[i * 2 + 1]; val xj = xy[j * 2]; val yj = xy[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }

    /** Strokes count when most of their samples are inside; objects when their center is. */
    fun selects(item: Item): Boolean {
        if (!bounds.intersects(item.bounds)) return false
        return when (item) {
            is Stroke -> {
                val p = item.points
                val n = item.pointCount
                val step = maxOf(1, n / 24)
                var inside = 0; var total = 0
                var i = 0
                while (i < n) { total++; if (contains(p[i * 3], p[i * 3 + 1])) inside++; i += step }
                inside * 2 > total
            }
            else -> contains(item.bounds.centerX, item.bounds.centerY)
        }
    }

    val isUsable: Boolean get() = xy.size >= 6 && bounds.width * bounds.height > 4f
}
