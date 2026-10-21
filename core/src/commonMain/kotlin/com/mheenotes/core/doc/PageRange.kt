package com.mheenotes.core.doc

/**
 * Parses "1-3, 5, 8-" style ranges (1-based, inclusive) into sorted 0-based indices.
 * Returns null when nothing valid is selected.
 */
fun parsePageRange(text: String, pageCount: Int): List<Int>? {
    val out = sortedSetOf<Int>()
    for (part in text.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }) {
        val bounds = part.split('-', '–').map { it.trim() }
        val a = bounds[0].toIntOrNull() ?: if (bounds[0].isEmpty()) 1 else return null
        val b = if (bounds.size == 1) a else bounds[1].toIntOrNull() ?: if (bounds[1].isEmpty()) pageCount else return null
        for (p in minOf(a, b)..maxOf(a, b)) if (p in 1..pageCount) out += p - 1
    }
    return out.toList().ifEmpty { null }
}
