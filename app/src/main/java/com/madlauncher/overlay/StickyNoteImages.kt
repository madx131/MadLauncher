package com.madlauncher.overlay

import android.content.Context
import android.net.Uri
import java.io.File

/** Copies gallery picks into app storage so sticky notes do not keep a URI permission. */
object StickyNoteImages {
    private const val ROOT = "sticky_notes"

    fun copyFromUri(context: Context, noteId: String, uri: Uri): String? {
        val dir = File(context.filesDir, "$ROOT/$noteId").apply { mkdirs() }
        val dest = File(dir, "img_${System.currentTimeMillis()}.bin")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            dest.takeIf { it.exists() && it.length() > 0L }?.absolutePath
        }.getOrNull()
        if (copied == null && dest.exists()) dest.delete()
        return copied
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.isFile }?.delete() }
    }

    fun deleteAll(context: Context, noteId: String) {
        runCatching { File(context.filesDir, "$ROOT/$noteId").deleteRecursively() }
    }
}
