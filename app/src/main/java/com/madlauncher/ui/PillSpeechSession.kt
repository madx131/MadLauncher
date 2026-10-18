package com.madlauncher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Listens without leaving the launcher. The system speech activity was closing
 * the Action Box (home treats that as leaving) and the spoken text never arrived.
 */
internal class PillSpeechSession(private val context: Context) {
    var onPartial: (String) -> Unit = {}
    var onFinal: (String) -> Unit = {}
    var onStopped: () -> Unit = {}
    var onError: (Int) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        val gen = ++generation
        release()
        if (!available()) {
            onError(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        val session = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = session
        var latest = ""
        var delivered = false
        session.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                if (gen != generation) return
                val text = firstResult(partialResults) ?: return
                latest = text
                onPartial(text)
            }

            override fun onResults(results: Bundle?) {
                if (gen != generation || delivered) return
                val text = firstResult(results)?.takeIf { it.isNotBlank() } ?: latest
                delivered = true
                onStopped()
                if (text.isNotBlank()) onFinal(text)
                release()
            }

            override fun onError(error: Int) {
                if (gen != generation || delivered) return
                val usable = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (usable && latest.isNotBlank()) {
                    delivered = true
                    onStopped()
                    onFinal(latest)
                } else {
                    delivered = true
                    onStopped()
                    onError(error)
                }
                release()
            }
        })
        val prompt = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        runCatching { session.startListening(prompt) }
            .onFailure {
                generation++
                release()
                onError(SpeechRecognizer.ERROR_CLIENT)
            }
    }

    fun stop() {
        generation++
        release()
    }

    private fun release() {
        val session = recognizer ?: return
        recognizer = null
        main.post {
            runCatching { session.cancel() }
            runCatching { session.destroy() }
        }
    }

    private fun firstResult(bundle: Bundle?): String? {
        val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return null
        return list.firstOrNull { it.isNotBlank() }?.trim()
    }
}
