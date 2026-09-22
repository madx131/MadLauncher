package com.madlauncher.action

import android.content.res.AssetManager
import java.nio.charset.StandardCharsets

data class InstantModule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val offline: Boolean,
    val answerType: String,
    val examples: List<String>,
)

/**
 * Local Instant Answer modules (calc / conversions / translate).
 * Flavor pack lives in `assets/instant/`; defaults stay enabled when the pack is missing
 * (launcher flavor).
 */
object InstantModules {
    const val CALC = "calc"
    const val CONVERSIONS = "conversions"
    const val TRANSLATE = "translate"
    const val ASSET_DIR = "instant"
    const val LOCAL_SOURCE = "On device"

    private val defaultModules = listOf(
        InstantModule(CALC, "Calculator", enabled = true, offline = true, answerType = "calc", examples = listOf("2+2")),
        InstantModule(CONVERSIONS, "Unit conversion", enabled = true, offline = true, answerType = "conversions", examples = listOf("72 f to c")),
        InstantModule(TRANSLATE, "Translator", enabled = true, offline = true, answerType = "translate", examples = listOf("/t hello")),
    )

    @Volatile
    private var modules: List<InstantModule> = defaultModules

    fun resetToDefaults() {
        modules = defaultModules
    }

    fun isEnabled(id: String): Boolean =
        modules.firstOrNull { it.id == id }?.enabled ?: true

    fun moduleIdFor(result: CalcResult): String =
        if (result.kind == CalcKind.CONVERSION) CONVERSIONS else CALC

    fun loadFromAssets(assets: AssetManager) {
        val manifest = readAsset(assets, "$ASSET_DIR/manifest.json")
        if (manifest.isNullOrBlank()) {
            resetToDefaults()
            return
        }
        loadFromManifest(manifest) { file ->
            readAsset(assets, "$ASSET_DIR/$file")
        }
    }

    fun loadFromManifest(manifestJson: String, readModule: (String) -> String?) {
        val files = Regex("\"file\"\\s*:\\s*\"([^\"]+)\"").findAll(manifestJson)
            .map { it.groupValues[1] }
            .toList()
        if (files.isEmpty()) {
            resetToDefaults()
            return
        }
        val loaded = files.mapNotNull { file ->
            val raw = readModule(file) ?: return@mapNotNull null
            parseModule(raw)
        }
        modules = if (loaded.isEmpty()) defaultModules else loaded
    }

    fun localAnswer(query: String): InstantAnswer? {
        val parsed = CalcParser.tryParse(query) ?: return null
        if (!isEnabled(moduleIdFor(parsed))) return null
        val extras = parsed.extras.joinToString(" · ")
        val text = if (extras.isBlank()) parsed.answer else "${parsed.answer} ($extras)"
        return InstantAnswer(
            heading = parsed.expression,
            text = text,
            source = LOCAL_SOURCE,
            url = "",
            fromDuckDuckGo = false,
        )
    }

    private fun parseModule(json: String): InstantModule? {
        val id = InstantAnswerClient.jsonField(json, "id").ifBlank { return null }
        val enabled = booleanField(json, "enabled", default = true)
        return InstantModule(
            id = id,
            name = InstantAnswerClient.jsonField(json, "name").ifBlank { id },
            enabled = enabled,
            offline = booleanField(json, "offline", default = true),
            answerType = InstantAnswerClient.jsonField(json, "answerType").ifBlank { id },
            examples = exampleFields(json),
        )
    }

    private fun booleanField(json: String, key: String, default: Boolean): Boolean {
        val m = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(true|false)").find(json) ?: return default
        return m.groupValues[1] == "true"
    }

    private fun exampleFields(json: String): List<String> {
        val block = Regex("\"examples\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .find(json)?.groupValues?.get(1) ?: return emptyList()
        return Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
            .findAll(block)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun readAsset(assets: AssetManager, path: String): String? =
        runCatching {
            assets.open(path).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }.getOrNull()
}
