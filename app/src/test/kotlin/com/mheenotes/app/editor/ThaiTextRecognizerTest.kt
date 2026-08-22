package com.mheenotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ThaiTextRecognizerTest {
    @Test
    fun removesSystematicSpacesBetweenThaiCharacters() {
        assertEquals("ภาษาไทย", normalizeThaiOcrText("ภ า ษ า ไ ท ย"))
    }

    @Test
    fun preservesNormalPhraseSpacing() {
        assertEquals("ภาษาไทย ทดสอบ OCR", normalizeThaiOcrText("ภาษาไทย ทดสอบ OCR"))
    }
}
