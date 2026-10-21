package com.madlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableIntStateOf
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Installed icon styles under `filesDir/icon-styles/{id}/`.
 * [active] is what [com.madlauncher.ui.AppIcon] draws.
 */
object IconStyleStore {
    const val ACTION_INSTALL = IconStyleContract.ACTION_INSTALL

    data class Summary(val id: String, val name: String)

    data class Designer(val packageName: String, val className: String, val label: String)

    data class Installed(
        val id: String,
        val name: String,
        val mask: IconStyleContract.Mask?,
        val icons: Map<String, File>,
    )

    val revision = mutableIntStateOf(0)

    @Volatile
    var active: Installed? = null
        private set

    private val listeners = mutableListOf<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners -= listener
    }

    fun stylesDir(context: Context): File = File(context.filesDir, "icon-styles").apply { mkdirs() }

    fun list(context: Context): List<Summary> {
        val root = stylesDir(context)
        return root.listFiles()?.mapNotNull { dir ->
            if (!dir.isDirectory) return@mapNotNull null
            val spec = readSpec(dir) ?: return@mapNotNull null
            Summary(spec.id, spec.name)
        }.orEmpty().sortedBy { it.name.lowercase() }
    }

    fun designers(context: Context): List<Designer> {
        val pm = context.packageManager
        val intent = Intent(ACTION_INSTALL)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val cls = info.activityInfo.name ?: return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                Designer(pkg, cls, label)
            }
            .distinctBy { it.packageName to it.className }
            .sortedBy { it.label.lowercase() }
    }

    fun iconPath(packageName: String, activityName: String?): String? {
        val installed = active ?: return null
        val keys = ArrayList<String>(4)
        if (!activityName.isNullOrBlank()) {
            val short = if (activityName.startsWith(packageName)) {
                activityName.removePrefix(packageName)
            } else {
                activityName
            }
            val relative = if (short.startsWith(".")) short else ".$short"
            keys += "$packageName/$relative"
            keys += "$packageName/$activityName"
            keys += activityName
        }
        keys += packageName
        return keys.firstNotNullOfOrNull { key ->
            installed.icons[key]?.takeIf { it.isFile }?.absolutePath
                ?: installed.icons.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
                    ?.takeIf { it.isFile }?.absolutePath
        }
    }

    /**
     * Load [id] from disk. Blank clears the style. A missing folder clears it and
     * tells listeners the id is now blank. [force] reloads even when the id matches.
     */
    fun activate(context: Context, id: String, force: Boolean = false) {
        val clean = id.trim()
        if (!force && clean == active?.id.orEmpty()) return
        if (clean.isEmpty()) {
            active = null
            publish("")
            return
        }
        val dir = File(stylesDir(context), clean)
        val installed = readInstalled(dir)
        if (installed == null) {
            active = null
            publish("")
            return
        }
        active = installed
        publish(installed.id)
    }

    fun delete(context: Context, id: String) {
        val clean = id.trim()
        if (!clean.matches(Regex("^[a-z0-9_-]{1,64}$"))) return
        File(stylesDir(context), clean).deleteRecursively()
        if (active?.id == clean) {
            active = null
            publish("")
        } else {
            revision.intValue++
        }
    }

    /** Copy a style zip into `icon-styles/{id}`. Does not select it. */
    fun import(context: Context, uri: Uri): IconStyleContract.Spec? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > IconStyleContract.MAX_ZIP_BYTES) return null
        val entries = LinkedHashMap<String, ByteArray>()
        var total = 0
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.replace('\\', '/').trim().removePrefix("/")
                    if (entry.isDirectory) continue
                    if (!IconStyleContract.isSafeRelativePath(name)) return null
                    val data = zip.readBytes()
                    if (data.size > IconStyleContract.MAX_ENTRY_BYTES) return null
                    total += data.size
                    if (total > IconStyleContract.MAX_ZIP_BYTES) return null
                    entries[name] = data
                }
            }
        } catch (_: Exception) {
            return null
        }
        val json = entries["style.json"] ?: return null
        val spec = IconStyleContract.parse(json.toString(Charsets.UTF_8)) ?: return null
        for (path in spec.icons.values) {
            val data = entries[path] ?: return null
            if (!imageWithinLimit(data)) return null
        }
        val dir = File(stylesDir(context), spec.id)
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()
        val root = dir.canonicalFile
        File(dir, "style.json").writeBytes(json)
        for (path in spec.icons.values) {
            val dest = File(dir, path)
            dest.parentFile?.mkdirs()
            val rootPath = root.path.trimEnd('/', '\\') + File.separator
            if (!dest.canonicalFile.path.startsWith(rootPath)) {
                dir.deleteRecursively()
                return null
            }
            dest.writeBytes(entries.getValue(path))
        }
        return spec
    }

    fun uriFrom(intent: Intent?): Uri? {
        if (intent == null) return null
        val stream = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return stream ?: intent.data
    }

    private fun publish(id: String) {
        revision.intValue++
        listeners.toList().forEach { it(id) }
    }

    private fun readSpec(dir: File): IconStyleContract.Spec? {
        val file = File(dir, "style.json")
        if (!file.isFile) return null
        val spec = IconStyleContract.parse(runCatching { file.readText() }.getOrNull() ?: return null) ?: return null
        if (spec.id != dir.name) return null
        return spec
    }

    private fun readInstalled(dir: File): Installed? {
        val spec = readSpec(dir) ?: return null
        val icons = spec.icons.mapValues { (_, relative) -> File(dir, relative) }
        return Installed(spec.id, spec.name, spec.mask, icons)
    }

    private fun imageWithinLimit(bytes: ByteArray): Boolean {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return opts.outWidth in 1..IconStyleContract.MAX_ICON_PX &&
            opts.outHeight in 1..IconStyleContract.MAX_ICON_PX
    }
}
