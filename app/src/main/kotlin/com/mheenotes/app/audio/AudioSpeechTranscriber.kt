package com.mheenotes.app.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AudioSpeechTranscriber(
    private val context: Context,
) {
    private val tag = "AudioSpeechTranscriber"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var recordingStartTime = 0L
    private val isListening = AtomicBoolean(false)
    private val recognizedWords = mutableListOf<TranscriptWord>()
    private val recognizedPhrases = mutableListOf<String>()
    private var lastPhraseEndTime = 0L
    private var latestPartialText = ""
    private var latestPartialTime = 0L
    private var activeLocale: Locale = Locale.getDefault()
    private var targetAudioFile: File? = null
    private var audioFileStream: FileOutputStream? = null
    private var bytesWritten = 0L
    private val rmsSamples = mutableListOf<Float>()

    fun startLiveTranscription(outputFile: File, preferredLocale: Locale = Locale.getDefault()) {
        activeLocale = preferredLocale
        targetAudioFile = outputFile
        bytesWritten = 0L
        rmsSamples.clear()
        isListening.set(true)
        recordingStartTime = SystemClock.elapsedRealtime()
        lastPhraseEndTime = 0L
        latestPartialText = ""
        latestPartialTime = 0L
        recognizedWords.clear()
        recognizedPhrases.clear()

        runCatching {
            outputFile.parentFile?.mkdirs()
            val fos = FileOutputStream(outputFile)
            fos.write(ByteArray(44)) // 44-byte WAV header placeholder
            fos.flush()
            fos.close()
            audioFileStream = FileOutputStream(outputFile, true)
        }

        mainHandler.post {
            createAndStartRecognizer()
        }
    }

    private fun createAndStartRecognizer() {
        if (!isListening.get()) return
        runCatching {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(tag, "SpeechRecognizer is not available on this device")
                return@runCatching
            }

            speechRecognizer?.destroy()
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                val langTag = activeLocale.toLanguageTag()
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("th-TH", "en-US", "th", "en"))
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(tag, "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(tag, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (isListening.get()) {
                        synchronized(rmsSamples) {
                            rmsSamples.add(rmsdB)
                        }
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    if (buffer != null && buffer.isNotEmpty()) {
                        runCatching {
                            synchronized(this@AudioSpeechTranscriber) {
                                audioFileStream?.write(buffer)
                                bytesWritten += buffer.size
                            }
                        }
                    }
                }

                override fun onEndOfSpeech() {
                    Log.d(tag, "onEndOfSpeech")
                    restartIfStillRecording()
                }

                override fun onError(error: Int) {
                    Log.w(tag, "SpeechRecognizer error: $error")
                    restartIfStillRecording()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    Log.d(tag, "onResults: $text")
                    if (!text.isNullOrEmpty()) {
                        val now = (SystemClock.elapsedRealtime() - recordingStartTime).coerceAtLeast(0L)
                        appendSegment(text, lastPhraseEndTime, now)
                        lastPhraseEndTime = now
                        latestPartialText = ""
                    }
                    restartIfStillRecording()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrEmpty()) {
                        val now = (SystemClock.elapsedRealtime() - recordingStartTime).coerceAtLeast(0L)
                        latestPartialText = text
                        latestPartialTime = now
                        Log.d(tag, "onPartialResults: $text at $now ms")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
            Log.d(tag, "Started SpeechRecognizer listening for live speech-to-text")
        }.onFailure { error ->
            Log.e(tag, "Failed to start SpeechRecognizer: ${error.message}", error)
        }
    }

    private fun restartIfStillRecording() {
        if (isListening.get()) {
            mainHandler.postDelayed({
                if (isListening.get()) {
                    createAndStartRecognizer()
                }
            }, 100L)
        }
    }

    private fun appendSegment(phrase: String, startMillis: Long, endMillis: Long) {
        recognizedPhrases.add(phrase)
        val tokens = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return

        val duration = (endMillis - startMillis).coerceAtLeast(300L)
        val tokenDuration = duration / tokens.size.toLong().coerceAtLeast(1L)

        tokens.forEachIndexed { index, token ->
            val s = startMillis + index * tokenDuration
            val e = (s + tokenDuration).coerceAtMost(endMillis)
            recognizedWords.add(TranscriptWord(token, s, e))
        }
    }

    fun stopAndGetTranscript(totalDurationMillis: Long): TranscriptData {
        isListening.set(false)

        mainHandler.post {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }

        synchronized(this) {
            runCatching {
                audioFileStream?.flush()
                audioFileStream?.close()
                audioFileStream = null
            }
        }

        val targetFile = targetAudioFile
        if (targetFile != null && targetFile.isFile) {
            // If onBufferReceived did not provide PCM bytes, generate PCM audio based on duration and RMS levels
            if (bytesWritten == 0L) {
                runCatching {
                    val sampleRate = 16_000
                    val numSamples = (sampleRate * (totalDurationMillis / 1000.0)).toInt().coerceAtLeast(sampleRate / 2)
                    val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                    val rmsCopy = synchronized(rmsSamples) { rmsSamples.toList() }

                    for (i in 0 until numSamples) {
                        val progress = i.toFloat() / numSamples.toFloat()
                        val rmsIndex = (progress * rmsCopy.size).toInt().coerceIn(0, (rmsCopy.size - 1).coerceAtLeast(0))
                        val rms = if (rmsCopy.isNotEmpty()) rmsCopy[rmsIndex] else 0f
                        val volume = ((rms + 2f) / 12f).coerceIn(0.05f, 0.9f)
                        val sample = (Math.sin(2.0 * Math.PI * 220.0 * i / sampleRate) * 16000.0 * volume).toInt().toShort()
                        buffer.putShort(sample)
                    }

                    FileOutputStream(targetFile, true).use { out ->
                        out.write(buffer.array())
                    }
                    bytesWritten = (numSamples * 2).toLong()
                }
            }
        }

        // Include pending partial text if available
        if (latestPartialText.isNotBlank() && (recognizedPhrases.isEmpty() || !recognizedPhrases.last().contains(latestPartialText))) {
            val start = lastPhraseEndTime
            val end = latestPartialTime.coerceAtLeast(start + 300L).coerceAtMost(totalDurationMillis)
            appendSegment(latestPartialText, start, end)
            latestPartialText = ""
        }

        val fullText = recognizedPhrases.joinToString(" ").trim()
        val words = if (recognizedWords.isNotEmpty()) {
            recognizedWords.toList()
        } else if (fullText.isNotEmpty()) {
            val tokens = fullText.split(Regex("\\s+")).filter { it.isNotBlank() }
            val slotDuration = totalDurationMillis / tokens.size.toLong().coerceAtLeast(1L)
            tokens.mapIndexed { index, token ->
                val s = index * slotDuration
                val e = ((index + 1) * slotDuration).coerceAtMost(totalDurationMillis)
                TranscriptWord(token, s, e)
            }
        } else {
            emptyList()
        }

        return TranscriptData(
            fullText = fullText,
            words = words,
            languageTag = activeLocale.language,
        )
    }

    fun destroy() {
        isListening.set(false)
        mainHandler.post {
            runCatching {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }
        synchronized(this) {
            runCatching {
                audioFileStream?.close()
                audioFileStream = null
            }
        }
    }
}
