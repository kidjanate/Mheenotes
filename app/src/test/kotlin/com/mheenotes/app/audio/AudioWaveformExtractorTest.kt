package com.mheenotes.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioWaveformExtractorTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun extractsAmplitudesFromSyntheticWav() {
        val wavFile = tempFolder.newFile("test_audio.wav")
        createSyntheticWav(wavFile, sampleRate = 16_000, durationSeconds = 1)

        val bars = AudioWaveformExtractor.extractAmplitudes(wavFile, barCount = 40)
        assertEquals(40, bars.size)
        bars.forEach { amp ->
            assertTrue("Amplitude should be between 0.05 and 1.0, was $amp", amp in 0.05f..1.0f)
        }
    }

    @Test
    fun handlesEmptyOrMissingFileGracefully() {
        val nonExistent = File(tempFolder.root, "missing.wav")
        val bars = AudioWaveformExtractor.extractAmplitudes(nonExistent, barCount = 50)
        assertEquals(50, bars.size)
    }

    private fun createSyntheticWav(file: File, sampleRate: Int, durationSeconds: Int) {
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 for PCM)
        buffer.putShort(1) // NumChannels (1 = Mono)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2) // ByteRate
        buffer.putShort(2) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)

        // Generate synthetic sine wave with quiet and loud sections
        for (i in 0 until numSamples) {
            val volume = if (i < numSamples / 2) 0.1 else 0.8
            val sample = (Math.sin(2.0 * Math.PI * 440.0 * i / sampleRate) * 32767.0 * volume).toInt().toShort()
            buffer.putShort(sample)
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
    }
}
