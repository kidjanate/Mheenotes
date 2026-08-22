package com.mheenotes.app.audio

data class TranscriptWord(
    val text: String,
    val startMillis: Long,
    val endMillis: Long,
)

data class TranscriptData(
    val fullText: String,
    val words: List<TranscriptWord> = emptyList(),
    val languageTag: String? = null,
) {
    fun toJson(): String {
        if (fullText.isBlank()) return ""
        return buildString {
            append("{\"fullText\":")
            append(escapeJson(fullText))
            if (languageTag != null) {
                append(",\"lang\":")
                append(escapeJson(languageTag))
            }
            append(",\"words\":[")
            words.forEachIndexed { i, w ->
                if (i > 0) append(",")
                append("{\"t\":")
                append(escapeJson(w.text))
                append(",\"s\":")
                append(w.startMillis)
                append(",\"e\":")
                append(w.endMillis)
                append("}")
            }
            append("]}")
        }
    }

    fun wordIndexAt(positionMillis: Long): Int? {
        if (words.isEmpty()) return null
        val found = words.indexOfFirst { positionMillis in it.startMillis..it.endMillis }
        if (found >= 0) return found
        val lastPassed = words.indexOfLast { it.startMillis <= positionMillis }
        return if (lastPassed >= 0) lastPassed else 0
    }

    companion object {
        fun fromRaw(raw: String, durationMillis: Long): TranscriptData {
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed == "{}" || trimmed.startsWith("{\"fullText\":\"\"")) {
                return TranscriptData(fullText = "", words = emptyList())
            }

            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val fullText = extractJsonStringField(trimmed, "fullText")
                val lang = extractJsonStringField(trimmed, "lang")
                val words = extractJsonWords(trimmed)

                if (fullText != null && fullText.isNotBlank()) {
                    return TranscriptData(
                        fullText = fullText,
                        words = if (words.isNotEmpty()) words else estimateWordTimings(fullText, durationMillis),
                        languageTag = lang,
                    )
                }
            }


            return TranscriptData(
                fullText = trimmed,
                words = estimateWordTimings(trimmed, durationMillis),
                languageTag = null,
            )
        }

        private fun estimateWordTimings(text: String, durationMillis: Long): List<TranscriptWord> {
            val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return emptyList()

            val safeDuration = durationMillis.coerceAtLeast(1_000L)
            val slotDuration = safeDuration / tokens.size.toLong()

            return tokens.mapIndexed { index, token ->
                val start = index * slotDuration
                val end = ((index + 1) * slotDuration).coerceAtMost(safeDuration)
                TranscriptWord(text = token, startMillis = start, endMillis = end)
            }
        }

        private fun escapeJson(value: String): String = buildString {
            append('"')
            for (c in value) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append('"')
        }

        private fun extractJsonStringField(json: String, field: String): String? {
            val regex = Regex("\"$field\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            val match = regex.find(json) ?: return null
            return unescapeJson(match.groupValues[1])
        }

        private fun extractJsonWords(json: String): List<TranscriptWord> {
            val words = mutableListOf<TranscriptWord>()
            val wordRegex = Regex("\\{\\s*\"t\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*\"s\"\\s*:\\s*(\\d+)\\s*,\\s*\"e\"\\s*:\\s*(\\d+)\\s*\\}")
            for (match in wordRegex.findAll(json)) {
                val t = unescapeJson(match.groupValues[1])
                val s = match.groupValues[2].toLongOrNull() ?: 0L
                val e = match.groupValues[3].toLongOrNull() ?: (s + 300L)
                if (t.isNotBlank()) {
                    words.add(TranscriptWord(t, s, e))
                }
            }
            return words
        }

        private fun unescapeJson(raw: String): String {
            var i = 0
            val sb = StringBuilder()
            while (i < raw.length) {
                val c = raw[i]
                if (c == '\\' && i + 1 < raw.length) {
                    when (val next = raw[i + 1]) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                        'u' -> {
                            if (i + 5 < raw.length) {
                                val hex = raw.substring(i + 2, i + 6)
                                hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                                i += 5
                            } else {
                                sb.append(next)
                                i++
                            }
                        }
                        else -> sb.append(next)
                    }
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}

