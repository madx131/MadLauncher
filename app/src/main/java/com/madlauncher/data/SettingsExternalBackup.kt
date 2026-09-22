package com.madlauncher.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * External copy of [mad_store] that survives app uninstall.
 * Primary: `/sdcard/MadLauncher/backup/mad_store.xml`
 * Fallback: Downloads via MediaStore.
 */
object SettingsExternalBackup {
    private const val TAG = "MadSettingsBackup"
    private const val BACKUP_DIR = "MadLauncher/backup"
    private const val BACKUP_FILE = "mad_store.xml"
    private const val PREFS_NAME = "mad_store"

    fun backupDir(): File =
        File(Environment.getExternalStorageDirectory(), BACKUP_DIR)

    fun backupFile(): File = File(backupDir(), BACKUP_FILE)

    /** Copy live SharedPreferences XML out of the app sandbox. */
    fun exportNow(context: Context): Boolean {
        val src = prefsFile(context) ?: return false
        if (!src.exists() || src.length() < 64) return false
        return writeExternal(context, src)
    }

    /**
     * If the live store looks empty/default and an external backup exists, restore it.
     * Returns true when prefs were replaced (caller should recreate LocalStore / restart UI).
     */
    fun restoreIfNeeded(context: Context): Boolean {
        val live = prefsFile(context)
        val backup = findReadableBackup(context) ?: return false
        if (backup.length() < 200) return false
        val liveLen = live?.takeIf { it.exists() }?.length() ?: 0L
        // Only auto-restore when local store is missing or clearly smaller than backup.
        if (liveLen >= backup.length() * 9 / 10 && liveLen > 2_000) return false
        return try {
            val destDir = File(context.applicationInfo.dataDir, "shared_prefs")
            destDir.mkdirs()
            val dest = File(destDir, "$PREFS_NAME.xml")
            FileInputStream(backup).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            // SharedPreferences may already be loaded in memory — force process restart is safest;
            // for cold start before LocalStore reads, this is enough.
            Log.i(TAG, "Restored settings from ${backup.absolutePath} (${backup.length()} bytes)")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "restore failed", t)
            false
        }
    }

    private fun prefsFile(context: Context): File? {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        val f = File(dir, "$PREFS_NAME.xml")
        return f
    }

    private fun writeExternal(context: Context, src: File): Boolean {
        val dirOk = runCatching {
            val dir = backupDir()
            if (!dir.exists()) dir.mkdirs()
            val dest = backupFile()
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.exists() && dest.length() > 0
        }.getOrDefault(false)
        if (dirOk) {
            Log.i(TAG, "Backup written ${backupFile().absolutePath}")
            return true
        }
        return writeDownloads(context, src).also {
            if (it) Log.i(TAG, "Backup written to Downloads")
        }
    }

    private fun findReadableBackup(context: Context): File? {
        val primary = backupFile()
        if (primary.exists() && primary.canRead() && primary.length() > 200) return primary
        // Older copies in Downloads
        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val candidates = listOf(
            File(dl, "MadLauncher-mad_store.xml"),
            File(dl, BACKUP_FILE),
            File(File(dl, "MadLauncher"), BACKUP_FILE),
        )
        return candidates.firstOrNull { it.exists() && it.canRead() && it.length() > 200 }
    }

    private fun writeDownloads(context: Context, src: File): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val name = "MadLauncher-mad_store.xml"
                resolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Downloads.DISPLAY_NAME}=?",
                    arrayOf(name),
                )
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/xml")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(src).use { it.copyTo(out) }
                } ?: return false
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val dest = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MadLauncher-mad_store.xml",
                )
                FileInputStream(src).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                dest.exists()
            }
        }.getOrDefault(false)
    }
}
