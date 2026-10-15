package com.madlauncher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

data class ActionBoxVoiceController(
    val listening: Boolean,
    val toggle: () -> Unit,
)

/** Mic → SpeechRecognizer → [onResult] with the best spoken phrase. */
@Composable
fun rememberActionBoxVoice(onResult: (String) -> Unit): ActionBoxVoiceController {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    val recognizerHolder = remember { arrayOfNulls<SpeechRecognizer>(1) }
    val onResultState = rememberUpdatedResult(onResult)

    fun destroyRecognizer() {
        runCatching { recognizerHolder[0]?.destroy() }
        recognizerHolder[0] = null
        listening = false
    }

    fun ensureRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        recognizerHolder[0]?.let { return it }
        val created = SpeechRecognizer.createSpeechRecognizer(context)
        created.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                listening = false
            }
            override fun onError(error: Int) {
                listening = false
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) onResultState(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizerHolder[0] = created
        return created
    }

    fun startListening() {
        val speech = ensureRecognizer() ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        listening = true
        runCatching { speech.startListening(intent) }.onFailure {
            listening = false
        }
    }

    fun stopListening() {
        runCatching { recognizerHolder[0]?.stopListening() }
        listening = false
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startListening()
    }

    DisposableEffect(Unit) {
        onDispose { destroyRecognizer() }
    }

    return ActionBoxVoiceController(
        listening = listening,
        toggle = {
            if (listening) {
                stopListening()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) startListening() else permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}

@Composable
private fun rememberUpdatedResult(onResult: (String) -> Unit): (String) -> Unit {
    val latest = remember { mutableStateOf(onResult) }
    latest.value = onResult
    return remember { { spoken: String -> latest.value(spoken) } }
}
