package com.mheenotes.core.geometry

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
    val isEmpty: Boolean get() = right <= left || bottom <= top

    fun intersects(o: Rect): Boolean = left < o.right && o.left < right && top < o.bottom && o.top < bottom
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
    fun outset(d: Float) = Rect(left - d, top - d, right + d, bottom + d)
    fun offset(dx: Float, dy: Float) = Rect(left + dx, top + dy, right + dx, bottom + dy)
    fun union(o: Rect) = Rect(min(left, o.left), min(top, o.top), max(right, o.right), max(bottom, o.bottom))
    fun normalized() = Rect(min(left, right), min(top, bottom), max(left, right), max(top, bottom))

    /** Axis-aligned bounds after rotating this rect by [degrees] about its center. */
    fun rotatedBounds(degrees: Float): Rect {
        if (degrees == 0f) return this
        val r = degrees.toRadians()
        val c = abs(cos(r))
        val s = abs(sin(r))
        val hw = (width * c + height * s) / 2
        val hh = (width * s + height * c) / 2
        return Rect(centerX - hw, centerY - hh, centerX + hw, centerY + hh)
    }

    companion object {
        val Empty = Rect(0f, 0f, 0f, 0f)
        fun around(x: Float, y: Float, r: Float) = Rect(x - r, y - r, x + r, y + r)
    }
}

fun Float.toRadians(): Float = this * (kotlin.math.PI.toFloat() / 180f)

/** Similarity transform: rotate and uniformly scale about a pivot, then translate. */
data class Similarity(
    val pivotX: Float,
    val pivotY: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val dx: Float = 0f,
    val dy: Float = 0f,
) {
    private val c = cos(rotation.toRadians()) * scale
    private val s = sin(rotation.toRadians()) * scale

    fun mapX(x: Float, y: Float): Float = pivotX + (x - pivotX) * c - (y - pivotY) * s + dx
    fun mapY(x: Float, y: Float): Float = pivotY + (x - pivotX) * s + (y - pivotY) * c + dy

    val isIdentity: Boolean get() = scale == 1f && rotation == 0f && dx == 0f && dy == 0f
}
