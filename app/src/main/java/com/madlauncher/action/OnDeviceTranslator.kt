package com.madlauncher.action

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import kotlinx.coroutines.tasks.await

sealed class OnDeviceTranslateUi {
    data object Idle : OnDeviceTranslateUi()
    data class Working(val message: String) : OnDeviceTranslateUi()
    data class Done(val text: String, val from: String, val to: String) : OnDeviceTranslateUi()
    data class Error(val message: String) : OnDeviceTranslateUi()
}

object OnDeviceTranslator {
    @Volatile
    private var cached: Pair<String, Translator>? = null

    fun mapCode(code: String?): String? {
        val raw = code?.trim()?.lowercase(Locale.US).orEmpty()
        if (raw.isEmpty() || raw == "auto" || raw == "detect" || raw == "automatic") return null
        val tag = when (raw) {
            "zh-cn", "zhcn", "zh" -> "zh"
            "zh-tw", "zhtw" -> "zh"
            "pt-br", "ptbr" -> "pt"
            "fil", "tagalog" -> "tl"
            "iw" -> "he"
            "nb", "nn" -> "no"
            else -> raw.substringBefore('-')
        }
        return TranslateLanguage.fromLanguageTag(tag)
    }

    suspend fun translate(
        text: String,
        fromCode: String?,
        toCode: String?,
    ): OnDeviceTranslateUi {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return OnDeviceTranslateUi.Idle
        val target = mapCode(toCode) ?: return OnDeviceTranslateUi.Idle
        val detected = mapCode(fromCode) ?: detectLanguage(trimmed)
        val source = detected ?: TranslateLanguage.ENGLISH
        val key = "$source>$target"
        val translator = cached?.takeIf { it.first == key }?.second ?: run {
            cached?.second?.close()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
            Translation.getClient(options).also { cached = key to it }
        }
        return runCatching {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            val out = translator.translate(trimmed).await()
            OnDeviceTranslateUi.Done(out, source, target)
        }.getOrElse { err ->
            OnDeviceTranslateUi.Error(err.message?.take(120) ?: "Couldn't translate on device")
        }
    }

    private suspend fun detectLanguage(text: String): String? {
        val tag = runCatching {
            LanguageIdentification.getClient().identifyLanguage(text).await()
        }.getOrNull()
        if (tag.isNullOrBlank() || tag == "und") return null
        return TranslateLanguage.fromLanguageTag(tag)
    }
}
