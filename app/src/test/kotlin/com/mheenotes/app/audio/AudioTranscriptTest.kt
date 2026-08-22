package com.mheenotes.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioTranscriptTest {
    @Test
    fun serializesAndDeserializesStructuredTranscript() {
        val original = TranscriptData(
            fullText = "สวัสดีครับ วันนี้เราจะมาเรียนเรื่อง OCR",
            words = listOf(
                TranscriptWord("สวัสดีครับ", 0L, 800L),
                TranscriptWord("วันนี้เราจะมาเรียนเรื่อง", 850L, 2500L),
                TranscriptWord("OCR", 2600L, 3200L),
            ),
            languageTag = "th",
        )

        val json = original.toJson()
        val parsed = TranscriptData.fromRaw(json, durationMillis = 3500L)

        assertEquals("สวัสดีครับ วันนี้เราจะมาเรียนเรื่อง OCR", parsed.fullText)
        assertEquals("th", parsed.languageTag)
        assertEquals(3, parsed.words.size)
        assertEquals("สวัสดีครับ", parsed.words[0].text)
        assertEquals(0L, parsed.words[0].startMillis)
        assertEquals(800L, parsed.words[0].endMillis)
        assertEquals("OCR", parsed.words[2].text)
        assertEquals(2600L, parsed.words[2].startMillis)
    }

    @Test
    fun legacyPlainTextFallbackEstimatesWordTimings() {
        val plainText = "Hello world from Mheenotes audio note"
        val parsed = TranscriptData.fromRaw(plainText, durationMillis = 6000L)

        assertEquals("Hello world from Mheenotes audio note", parsed.fullText)
        assertEquals(6, parsed.words.size)
        assertEquals("Hello", parsed.words[0].text)
        assertEquals(0L, parsed.words[0].startMillis)
        assertEquals(1000L, parsed.words[0].endMillis)
        assertEquals("note", parsed.words[5].text)
    }

    @Test
    fun resolvesWordIndexAtPosition() {
        val transcript = TranscriptData(
            fullText = "Word One Two Three",
            words = listOf(
                TranscriptWord("Word", 0L, 1000L),
                TranscriptWord("One", 1000L, 2000L),
                TranscriptWord("Two", 2000L, 3000L),
                TranscriptWord("Three", 3000L, 4000L),
            ),
        )

        assertEquals(0, transcript.wordIndexAt(0L))
        assertEquals(0, transcript.wordIndexAt(500L))
        assertEquals(1, transcript.wordIndexAt(1500L))
        assertEquals(2, transcript.wordIndexAt(2500L))
        assertEquals(3, transcript.wordIndexAt(3500L))
    }

    @Test
    fun handlesEmptyAndBlankTranscriptsCleanly() {
        val emptyData = TranscriptData(fullText = "", languageTag = "th")
        assertEquals("", emptyData.toJson())

        val parsedFromEmptyJson = TranscriptData.fromRaw("{\"fullText\":\"\",\"lang\":\"th\",\"words\":[]}", 5000L)
        assertEquals("", parsedFromEmptyJson.fullText)
        assertEquals(0, parsedFromEmptyJson.words.size)
    }
}

