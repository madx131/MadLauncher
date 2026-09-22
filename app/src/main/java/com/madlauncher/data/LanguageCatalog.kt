package com.madlauncher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

data class LanguagePackEntry(
    val fileName: String,
    val label: String,
    val sha: String? = null,
)

object LanguageCatalog {
    private const val TAG = "LanguageCatalog"
    /** Bundled copy of S:\\Madl\\Lang\\README.txt — human-readable labels fallback. */
    private const val BUNDLED_README = "lang/README.txt"
    const val GITHUB_LANG_DIR = "lang"

    /** Accepts `heb Hebrew` or `heb.json Hebrew` (and legacy 2-letter codes). */
    private val packLine = Regex("""^\s*([A-Za-z][A-Za-z0-9-]*)(?:\.json)?\s+(.+?)\s*$""")

    /** Bundled APK packs (except English). Always available without a GitHub token. */
    fun bundledEntries(context: Context, extraLabels: Map<String, String> = emptyMap()): List<LanguagePackEntry> {
        val labels = parseReadmeLabels(loadBundledReadme(context)) + extraLabels
        val chosen = linkedMapOf<String, String>()
        for (name in context.assets.list("lang").orEmpty()) {
            if (!name.endsWith(".json", ignoreCase = true)) continue
            if (name.equals("download-ids.json", ignoreCase = true)) continue
            if (LanguageCodes.isEnglishPack(name)) continue
            val migrated = LanguageCodes.migrateFileName(name)
            if (migrated.isBlank()) continue
            if (chosen[migrated] == null || name.equals(migrated, ignoreCase = true)) {
                chosen[migrated] = name
            }
        }
        return chosen.keys
            .map { fileName ->
                LanguagePackEntry(
                    fileName = fileName,
                    label = labels[fileName]
                        ?: labels[chosen[fileName].orEmpty()]
                        ?: labelFromFileName(fileName),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun fetchAvailable(context: Context): Result<List<LanguagePackEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val bundledLabels = parseReadmeLabels(loadBundledReadme(context))
            val bundled = bundledEntries(context, bundledLabels)
            var remoteLabels = emptyMap<String, String>()
            val remote = if (RepoContents.hasToken(context)) {
                val listed = RepoContents.listDir(context, GITHUB_LANG_DIR)
                val entries = listed.getOrElse { err ->
                    if (bundled.isNotEmpty()) return@runCatching bundled
                    throw err
                }
                val jsonFiles = entries.filter {
                    it.type == "file" &&
                        it.name.endsWith(".json", ignoreCase = true) &&
                        !LanguageCodes.isEnglishPack(it.name)
                }
                val remoteReadme = entries.firstOrNull {
                    it.name.equals("README.txt", ignoreCase = true)
                }
                remoteLabels = remoteReadme?.let { item ->
                    RepoContents.getText(context, item.path).getOrNull()?.let { parseReadmeLabels(it) }
                }.orEmpty()
                jsonFiles.map { item ->
                    val fileName = LanguageCodes.migrateFileName(item.name).ifBlank { item.name.lowercase() }
                    LanguagePackEntry(
                        fileName = fileName,
                        label = bundledLabels[fileName]
                            ?: remoteLabels[fileName]
                            ?: remoteLabels[item.name]
                            ?: remoteLabels[item.name.lowercase()]
                            ?: labelFromFileName(fileName),
                        sha = item.sha,
                    )
                }
            } else {
                emptyList()
            }
            val labels = bundledLabels + remoteLabels
            val byName = linkedMapOf<String, LanguagePackEntry>()
            for (entry in bundled) {
                byName[entry.fileName] = entry.copy(label = labels[entry.fileName] ?: entry.label)
            }
            for (entry in remote) {
                val label = labels[entry.fileName] ?: entry.label
                byName[entry.fileName] = entry.copy(label = label)
            }
            byName.values.sortedBy { it.label.lowercase() }.also { merged ->
                if (merged.isEmpty()) error("No language packs")
            }
        }
    }

    suspend fun downloadPack(context: Context, entry: LanguagePackEntry): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = LanguageCodes.migrateFileName(entry.fileName).ifBlank {
                error("English is built-in")
            }
            val dest = File(LanguageOverlay.deviceDir(context), fileName)
            if (entry.sha != null && dest.exists()) {
                val local = gitBlobSha(dest.readBytes())
                if (local == entry.sha) {
                    if (LanguageOverlay.parse(dest).isNotEmpty()) return@runCatching dest
                }
            }
            Log.d(TAG, "Downloading lang/$fileName")
            val bytes = RepoContents.getBytes(context, "$GITHUB_LANG_DIR/$fileName").getOrElse { err ->
                error(err.message ?: "Download failed for $fileName")
            }
            if (!isLanguageJsonBytes(bytes)) {
                val preview = bytes.toString(StandardCharsets.UTF_8).take(120).replace('\n', ' ')
                Log.w(TAG, "Bad payload for $fileName: $preview")
                error("Downloaded file is not a valid language pack")
            }
            dest.parentFile?.mkdirs()
            dest.writeBytes(bytes)
            if (LanguageOverlay.parse(dest).isEmpty()) error("Language pack has no strings")
            Log.d(TAG, "Saved $fileName (${bytes.size} bytes)")
            dest
        }
    }

    fun installedEntries(context: Context, labels: Map<String, String>): List<LanguagePackEntry> {
        return LanguageOverlay.scan(context)
            .map { file ->
                LanguagePackEntry(
                    fileName = file.name,
                    label = labels[file.name] ?: labelFromFileName(file.name),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun labelFromFileName(fileName: String): String {
        val code = fileName.removeSuffix(".json")
        return code.replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun parseReadmeLabels(text: String): Map<String, String> =
        parseReadme(text).associate { it.fileName to it.label }

    fun parseReadme(text: String): List<LanguagePackEntry> {
        val inLanguageSection = languageListSection(text)
        return inLanguageSection.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("-") || trimmed.startsWith("(")) return@mapNotNull null
                val match = packLine.matchEntire(trimmed) ?: return@mapNotNull null
                val fileName = LanguageCodes.migrateFileName(normalizeFileName(match.groupValues[1]))
                if (fileName.isBlank()) return@mapNotNull null
                val label = match.groupValues[2].trim()
                LanguagePackEntry(fileName, label)
            }
            .distinctBy { it.fileName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun languageListSection(text: String): String {
        val sliced = when {
            text.contains("Language codes") -> text.substringAfter("Language codes")
            text.contains("Language files") -> text.substringAfter("Language files")
            else -> text
        }
        return sliced
            .substringBefore("Machine-translated")
            .substringBefore("---download-ids---")
            .substringBefore("Drive folder")
            .substringBefore("GitHub folder")
            .substringBefore("Optional:")
            .substringBefore("Quality note")
    }

    private fun normalizeFileName(token: String): String =
        if (token.endsWith(".json", ignoreCase = true)) token.lowercase() else "${token.lowercase()}.json"

    private fun loadBundledReadme(context: Context): String =
        context.assets.open(BUNDLED_README).use { it.readBytes().toString(StandardCharsets.UTF_8) }

    private fun isLanguageJsonText(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith("{") && t.contains("\"app_name\"")
    }

    private fun isLanguageJsonBytes(bytes: ByteArray): Boolean {
        val head = bytes.toString(StandardCharsets.UTF_8).trimStart().take(256).lowercase()
        if (head.startsWith("<!doctype") || head.startsWith("<html")) return false
        return isLanguageJsonText(bytes.toString(StandardCharsets.UTF_8))
    }

    fun bundledBlobSha(context: Context, fileName: String): String? {
        val destName = LanguageCodes.migrateFileName(fileName).ifBlank { return null }
        val names = context.assets.list("lang").orEmpty().filter { name ->
            name.endsWith(".json", ignoreCase = true) &&
                LanguageCodes.migrateFileName(name) == destName
        }
        val asset = names.firstOrNull { it.equals(destName, ignoreCase = true) } ?: names.firstOrNull() ?: return null
        return runCatching {
            gitBlobSha(context.assets.open("lang/$asset").use { it.readBytes() })
        }.getOrNull()
    }

    fun gitBlobSha(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(StandardCharsets.UTF_8)
        val md = java.security.MessageDigest.getInstance("SHA-1")
        md.update(header)
        md.update(bytes)
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }
}
