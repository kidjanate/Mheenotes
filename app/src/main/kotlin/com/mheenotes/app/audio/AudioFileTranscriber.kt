package com.mheenotes.app.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object AudioFileTranscriber {
    private const val TAG = "AudioFileTranscriber"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun transcribe(
        context: Context,
        wavFile: File,
        durationMillis: Long,
        preferredLocale: Locale = Locale.getDefault(),
        onProgress: ((String) -> Unit)? = null,
        onComplete: (TranscriptData) -> Unit,
    ) {
        if (!wavFile.isFile || wavFile.length() <= 44L) {
            onComplete(TranscriptData("", emptyList(), preferredLocale.language))
            return
        }

        mainHandler.post {
            runCatching {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "SpeechRecognizer is not available on this device")
                    onComplete(TranscriptData("", emptyList(), preferredLocale.language))
                    return@runCatching
                }

                val pipe = ParcelFileDescriptor.createPipe()
                val readPipe = pipe[0]
                val writePipe = pipe[1]

                var recognizer: SpeechRecognizer? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                val recognizedPhrases = mutableListOf<String>()
                var latestPartialText = ""
                val completed = AtomicBoolean(false)
                val isStreamFinished = AtomicBoolean(false)

                fun buildCurrentFullText(): String {
                    val combined = recognizedPhrases.joinToString(" ").trim()
                    return if (combined.isNotBlank()) {
                        if (latestPartialText.isNotBlank() && !combined.endsWith(latestPartialText)) {
                            "$combined $latestPartialText".trim()
                        } else {
                            combined
                        }
                    } else {
                        latestPartialText.trim()
                    }
                }

                fun finishAndCallback() {
                    if (!completed.compareAndSet(false, true)) return
                    mainHandler.post {
                        runCatching {
                            recognizer?.stopListening()
                            recognizer?.destroy()
                            recognizer = null
                            readPipe.close()
                        }
                    }

                    val fullText = buildCurrentFullText()
                    val words = if (fullText.isNotBlank()) {
                        val tokens = fullText.split(Regex("\\s+")).filter { it.isNotBlank() }
                        val slotDuration = durationMillis.coerceAtLeast(500L) / tokens.size.toLong().coerceAtLeast(1L)
                        tokens.mapIndexed { index, token ->
                            val s = index * slotDuration
                            val e = ((index + 1) * slotDuration).coerceAtMost(durationMillis)
                            TranscriptWord(token, s, e)
                        }
                    } else {
                        emptyList()
                    }

                    val result = TranscriptData(
                        fullText = fullText,
                        words = words,
                        languageTag = preferredLocale.language,
                    )
                    mainHandler.post {
                        onComplete(result)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    val langTag = preferredLocale.toLanguageTag()
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("th-TH", "en-US", "th", "en"))
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readPipe)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
                }

                fun restartListeningIfNeeded() {
                    if (!completed.get() && !isStreamFinished.get()) {
                        mainHandler.postDelayed({
                            if (!completed.get() && !isStreamFinished.get()) {
                                runCatching { recognizer?.startListening(intent) }
                            }
                        }, 50L)
                    }
                }

                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "AudioFileTranscriber: onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "AudioFileTranscriber: onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "AudioFileTranscriber: onEndOfSpeech")
                        if (isStreamFinished.get()) {
                            mainHandler.postDelayed({ finishAndCallback() }, 500L)
                        } else {
                            restartListeningIfNeeded()
                        }
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "AudioFileTranscriber: onError $error")
                        if (isStreamFinished.get()) {
                            finishAndCallback()
                        } else {
                            restartListeningIfNeeded()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        Log.d(TAG, "AudioFileTranscriber onResults: $text")
                        if (!text.isNullOrEmpty() && !recognizedPhrases.contains(text)) {
                            recognizedPhrases.add(text)
                            latestPartialText = ""
                            mainHandler.post { onProgress?.invoke(buildCurrentFullText()) }
                        }

                        if (isStreamFinished.get()) {
                            mainHandler.postDelayed({ finishAndCallback() }, 500L)
                        } else {
                            restartListeningIfNeeded()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            latestPartialText = text
                            mainHandler.post { onProgress?.invoke(buildCurrentFullText()) }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer?.startListening(intent)

                // Stream PCM bytes from the WAV file into the write pipe
                Thread({
                    runCatching {
                        ParcelFileDescriptor.AutoCloseOutputStream(writePipe).use { outputStream ->
                            FileInputStream(wavFile).use { inputStream ->
                                // Skip standard 44-byte WAV header
                                val header = ByteArray(44)
                                inputStream.read(header)

                                val buffer = ByteArray(4096)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    if (completed.get()) break
                                    outputStream.write(buffer, 0, bytesRead)
                                    Thread.sleep(2L)
                                }
                                outputStream.flush()
                            }
                        }
                    }
                    isStreamFinished.set(true)
                    // After stream finishes, wait 1.5 seconds for final recognition events then complete
                    mainHandler.postDelayed({
                        finishAndCallback()
                    }, 1500L)
                }, "Mheenotes-wav-streamer").start()

                // Overall safety timeout
                mainHandler.postDelayed({
                    if (!completed.get()) {
                        finishAndCallback()
                    }
                }, (durationMillis + 8000L).coerceAtLeast(12000L))

            }.onFailure { error ->
                Log.e(TAG, "Failed to start AudioFileTranscriber: ${error.message}", error)
                onComplete(TranscriptData("", emptyList(), preferredLocale.language))
            }
        }
    }
}
