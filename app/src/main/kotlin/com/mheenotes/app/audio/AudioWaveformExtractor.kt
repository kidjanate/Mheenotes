package com.mheenotes.app.audio

import java.io.File
import java.io.FileInputStream
import kotlin.math.sqrt

object AudioWaveformExtractor {
    fun extractAmplitudes(wavFile: File, barCount: Int = 80): List<Float> {
        val defaultBars = List(barCount) { 0.15f }
        if (!wavFile.isFile || wavFile.length() <= 44L) {
            return defaultBars
        }

        return runCatching {
            val fileLength = wavFile.length()
            val dataBytes = (fileLength - 44L).coerceAtLeast(0L)
            val totalSamples = (dataBytes / 2L).toInt()
            if (totalSamples <= 0) return defaultBars

            val samplesPerBucket = (totalSamples / barCount).coerceAtLeast(1)
            val amplitudes = FloatArray(barCount)

            FileInputStream(wavFile).use { input ->
                val header = ByteArray(44)
                input.read(header)

                val buffer = ByteArray(samplesPerBucket * 2)
                var bucketIndex = 0

                while (bucketIndex < barCount) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) break

                    val samplesInThisBucket = bytesRead / 2
                    if (samplesInThisBucket <= 0) break

                    var sumSquares = 0.0
                    var maxPeak = 0

                    for (i in 0 until samplesInThisBucket) {
                        val low = buffer[i * 2].toInt() and 0xFF
                        val high = buffer[i * 2 + 1].toInt()
                        val sample = (high shl 8) or low
                        val absSample = if (sample < 0) -sample else sample

                        if (absSample > maxPeak) maxPeak = absSample
                        sumSquares += (sample.toDouble() * sample.toDouble())
                    }

                    val rms = sqrt(sumSquares / samplesInThisBucket)
                    // Blend RMS (70%) and Peak (30%) for responsive speech peaks
                    val energy = (rms * 0.70 + maxPeak * 0.30) / 32768.0
                    amplitudes[bucketIndex] = energy.toFloat()
                    bucketIndex++
                }
            }

            val maxEnergy = amplitudes.maxOrNull()?.coerceAtLeast(0.01f) ?: 1.0f
            // Normalize amplitudes between 0.08f (subtle quiet baseline) and 1.0f (speech peak)
            amplitudes.map { energy ->
                val normalized = (energy / maxEnergy).coerceIn(0.0f, 1.0f)
                val curved = sqrt(normalized.toDouble()).toFloat() // Slight curve to make quieter speech visible
                (0.08f + curved * 0.92f).coerceIn(0.08f, 1.0f)
            }
        }.getOrDefault(defaultBars)
    }
}
