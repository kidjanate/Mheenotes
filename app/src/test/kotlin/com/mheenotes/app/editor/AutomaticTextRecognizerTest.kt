package com.mheenotes.app.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticTextRecognizerTest {
    @Test
    fun latinTextPrefersLatinModel() {
        assertPreferred(OcrScript.Latin, "Résumé déjà vu")
    }

    @Test
    fun chineseTextPrefersCjkModelOverLatin() {
        val text = "中文识别测试"

        assertTrue(score(OcrScript.Chinese, text) > score(OcrScript.Latin, text))
    }

    @Test
    fun japaneseKanaPrefersJapaneseModel() {
        assertPreferred(OcrScript.Japanese, "ノートを読み取る")
    }

    @Test
    fun devanagariTextPrefersDevanagariModel() {
        assertPreferred(OcrScript.Devanagari, "हिन्दी पाठ पहचान")
    }

    @Test
    fun koreanTextPrefersKoreanModel() {
        assertPreferred(OcrScript.Korean, "한국어 텍스트 인식")
    }

    @Test
    fun thaiTextPrefersThaiModel() {
        assertPreferred(OcrScript.Thai, "อ่านข้อความภาษาไทย")
    }

    @Test
    fun thaiCandidateRequiresActualThaiText() {
        assertTrue(isReliableThaiOcr("อ่านข้อความภาษาไทย", meanConfidence = .72f))
        assertTrue(!isReliableThaiOcr("This is English text", meanConfidence = .95f))
        assertTrue(!isReliableThaiOcr("ภาษาไทย", meanConfidence = .2f))
    }

    private fun assertPreferred(expected: OcrScript, text: String) {
        val expectedScore = score(expected, text)
        OcrScript.entries.filterNot { it == expected }.forEach { other ->
            assertTrue("Expected $expected to beat $other for $text", expectedScore > score(other, text))
        }
    }

    private fun score(script: OcrScript, text: String): Double =
        automaticOcrScore(script, text, meanConfidence = .8f)
}
