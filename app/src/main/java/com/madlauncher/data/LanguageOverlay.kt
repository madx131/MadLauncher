package com.madlauncher.data

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONObject
import java.io.File
import java.util.Locale

object LanguageOverlay {
    @Volatile
    var selectedFile: String = ""
        private set

    @Volatile
    var map: Map<String, String> = emptyMap()
        private set

    /** Bumps when overlay map changes — observe in composables for live translation. */
    val revision = mutableIntStateOf(0)

    fun deviceDir(context: Context): File = File(context.filesDir, "lang").apply { mkdirs() }

    fun load(context: Context, fileName: String) {
        migrateInstalledPacks(context)
        val migrated = LanguageCodes.migrateFileName(fileName)
        selectedFile = migrated
        if (migrated.isBlank()) {
            map = emptyMap()
            revision.intValue++
            return
        }
        installBundledPack(context, migrated)
        val file = File(deviceDir(context), migrated)
        map = parse(file)
        revision.intValue++
    }

    /**
     * Rename on-device 2-letter packs (iw.json → heb.json) and drop English overlays.
     * Returns the migrated selected filename (blank = built-in English).
     */
    fun migrateInstalledPacks(context: Context): Map<String, String> {
        val dir = deviceDir(context)
        val renamed = mutableMapOf<String, String>()
        dir.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
            ?.forEach { file ->
                if (LanguageCodes.isEnglishPack(file.name)) {
                    file.delete()
                    renamed[file.name] = ""
                    return@forEach
                }
                val next = LanguageCodes.migrateFileName(file.name)
                if (next.isBlank() || next.equals(file.name, ignoreCase = true)) return@forEach
                val dest = File(dir, next)
                if (!dest.exists()) {
                    file.renameTo(dest)
                } else {
                    file.delete()
                }
                renamed[file.name] = next
            }
        return renamed
    }

    /** Action Box: follow the phone language using bundled or downloaded Lang packs. */
    fun applySystemLanguage(context: Context) {
        migrateInstalledPacks(context)
        val fileName = SystemLanguagePack.fileNameFor(systemLocale()) ?: run {
            load(context, "")
            return
        }
        if (!installBundledPack(context, fileName) && !File(deviceDir(context), fileName).exists()) {
            load(context, "")
            return
        }
        load(context, fileName)
    }

    fun systemLocale(): Locale {
        val config = Resources.getSystem().configuration
        return if (Build.VERSION.SDK_INT >= 24) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }

    /** Copy a bundled `assets/lang` pack into the device overlay dir. Overwrites a stale copy. */
    fun installBundledPack(context: Context, fileName: String): Boolean {
        val destName = LanguageCodes.migrateFileName(fileName)
        if (destName.isBlank()) return false
        val dest = File(deviceDir(context), destName)
        val assets = bundledAssetNames(context, destName)
        if (assets.isEmpty()) {
            return dest.exists() && parse(dest).isNotEmpty()
        }
        for (asset in assets) {
            val copied = runCatching {
                dest.parentFile?.mkdirs()
                context.assets.open("lang/$asset").use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest
            }.getOrNull()
            if (copied != null && parse(copied).isNotEmpty()) return true
        }
        return dest.exists() && parse(dest).isNotEmpty()
    }

    private fun bundledAssetNames(context: Context, destName: String): List<String> {
        val listed = context.assets.list("lang").orEmpty().filter { name ->
            name.endsWith(".json", ignoreCase = true) &&
                LanguageCodes.migrateFileName(name) == destName
        }
        return (listOf(destName) + listed).distinct()
    }

    fun scan(context: Context): List<File> {
        migrateInstalledPacks(context)
        val dir = deviceDir(context)
        return dir.listFiles { f ->
            f.isFile && f.name.endsWith(".json") && !LanguageCodes.isEnglishPack(f.name)
        }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    /** Removes downloaded overlay packs; built-in English needs no file. */
    fun clearInstalledPacks(context: Context) {
        deviceDir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") }?.forEach { it.delete() }
    }

    fun parse(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val obj = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        obj.keys().forEach { key -> out[key] = obj.optString(key) }
        return out
    }

    fun wrapGetString(resources: android.content.res.Resources, id: Int, fallback: () -> String): String {
        val name = runCatching { resources.getResourceEntryName(id) }.getOrNull() ?: return fallback()
        return map[name] ?: fallback()
    }
}
