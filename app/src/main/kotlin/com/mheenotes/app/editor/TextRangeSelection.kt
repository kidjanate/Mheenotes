package com.mheenotes.app.editor

import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import kotlin.math.hypot

internal data class RecognizedTextToken(
    val text: String,
    val bounds: PageRect,
    val lineIndex: Int,
) {
    init {
        require(text.isNotBlank()) { "Selectable OCR text cannot be blank" }
        require(lineIndex >= 0) { "OCR line index cannot be negative" }
    }
}

internal data class RecognizedPageText(
    val tokens: List<RecognizedTextToken>,
    val languageName: String,
)

internal enum class TextSelectionHandle { Start, End }

internal data class TextRangeSelection(
    val pageId: PageId,
    val tokens: List<RecognizedTextToken>,
    val startIndex: Int,
    val endIndex: Int,
    val languageName: String,
) {
    init {
        require(tokens.isNotEmpty()) { "A text selection needs at least one token" }
        require(startIndex in tokens.indices && endIndex in tokens.indices) {
            "Text selection indices must refer to recognized tokens"
        }
    }

    val firstSelectedIndex: Int get() = minOf(startIndex, endIndex)
    val lastSelectedIndex: Int get() = maxOf(startIndex, endIndex)

    fun move(handle: TextSelectionHandle, tokenIndex: Int): TextRangeSelection {
        require(tokenIndex in tokens.indices)
        return when (handle) {
            TextSelectionHandle.Start -> copy(startIndex = tokenIndex)
            TextSelectionHandle.End -> copy(endIndex = tokenIndex)
        }
    }

    fun selectAll(): TextRangeSelection = copy(startIndex = 0, endIndex = tokens.lastIndex)

    fun selectedText(): String = buildString {
        var previous: RecognizedTextToken? = null
        tokens.subList(firstSelectedIndex, lastSelectedIndex + 1).forEach { token ->
            previous?.let { before ->
                when {
                    before.lineIndex != token.lineIndex -> append('\n')
                    needsWordSeparator(before, token) -> append(' ')
                }
            }
            append(token.text)
            previous = token
        }
    }

    fun selectedLineBounds(): List<PageRect> {
        val lines = linkedMapOf<Int, PageRect>()
        tokens.subList(firstSelectedIndex, lastSelectedIndex + 1).forEach { token ->
            val existing = lines[token.lineIndex]
            lines[token.lineIndex] = if (existing == null) token.bounds else PageRect(
                left = minOf(existing.left, token.bounds.left),
                top = minOf(existing.top, token.bounds.top),
                right = maxOf(existing.right, token.bounds.right),
                bottom = maxOf(existing.bottom, token.bounds.bottom),
            )
        }
        return lines.values.toList()
    }

    fun contains(point: PagePoint, padding: Float = 0f): Boolean = selectedLineBounds().any { bounds ->
        point.x in (bounds.left - padding)..(bounds.right + padding) &&
            point.y in (bounds.top - padding)..(bounds.bottom + padding)
    }

    fun handlePosition(handle: TextSelectionHandle): PagePoint {
        val forward = startIndex <= endIndex
        val token = tokens[when (handle) {
            TextSelectionHandle.Start -> startIndex
            TextSelectionHandle.End -> endIndex
        }]
        val x = when (handle) {
            TextSelectionHandle.Start -> if (forward) token.bounds.left else token.bounds.right
            TextSelectionHandle.End -> if (forward) token.bounds.right else token.bounds.left
        }
        return PagePoint(x, token.bounds.bottom)
    }
}

internal fun nearestRecognizedTokenIndex(
    tokens: List<RecognizedTextToken>,
    point: PagePoint,
    maxDistance: Float = Float.POSITIVE_INFINITY,
): Int? = tokens.indices
    .map { index -> index to distanceTo(tokens[index].bounds, point) }
    .minByOrNull { it.second }
    ?.takeIf { it.second <= maxDistance }
    ?.first

private fun distanceTo(bounds: PageRect, point: PagePoint): Float {
    val dx = when {
        point.x < bounds.left -> bounds.left - point.x
        point.x > bounds.right -> point.x - bounds.right
        else -> 0f
    }
    val dy = when {
        point.y < bounds.top -> bounds.top - point.y
        point.y > bounds.bottom -> point.y - bounds.bottom
        else -> 0f
    }
    return hypot(dx, dy)
}

private fun needsWordSeparator(left: RecognizedTextToken, right: RecognizedTextToken): Boolean {
    val leftCodePoint = left.text.codePointBefore(left.text.length)
    val rightCodePoint = right.text.codePointAt(0)
    if (leftCodePoint.isThai() && rightCodePoint.isThai()) {
        if (rightCodePoint.isThaiCombiningOrTone()) return false
        val gap = (right.bounds.left - left.bounds.right).coerceAtLeast(0f)
        val averageHeight = (
            (left.bounds.bottom - left.bounds.top) + (right.bounds.bottom - right.bounds.top)
            ) / 2f
        return gap > averageHeight * .45f
    }
    return !leftCodePoint.usesContinuousWordSpacing() && !rightCodePoint.usesContinuousWordSpacing()
}

private fun Int.isThai(): Boolean = this in 0x0E00..0x0E7F

private fun Int.isThaiCombiningOrTone(): Boolean = this in 0x0E30..0x0E3A ||
    this in 0x0E45..0x0E4E

private fun Int.usesContinuousWordSpacing(): Boolean = this in 0x3040..0x30FF ||
    this in 0x31F0..0x31FF ||
    this in 0x3400..0x4DBF ||
    this in 0x4E00..0x9FFF ||
    this in 0xF900..0xFAFF ||
    this in 0xFF65..0xFF9F ||
    this in 0x1B000..0x1B16F ||
    this in 0x20000..0x2EBEF ||
    this in 0x30000..0x3134F

