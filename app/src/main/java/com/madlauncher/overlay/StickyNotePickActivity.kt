package com.madlauncher.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.madlauncher.MadApp

/** Gallery pick for a sticky note shown from the overlay service (no Activity Result there). */
class StickyNotePickActivity : ComponentActivity() {
    private val noteId: String by lazy { intent.getStringExtra(EXTRA_NOTE_ID).orEmpty() }

    private val picker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (noteId.isNotBlank() && uris.isNotEmpty()) {
            attach(uris)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (noteId.isBlank()) {
            finish()
            return
        }
        if (savedInstanceState == null) {
            runCatching { picker.launch("image/*") }
                .onFailure { finish() }
        }
    }

    private fun attach(uris: List<Uri>) {
        val store = MadApp.instance.store
        val paths = uris.mapNotNull { StickyNoteImages.copyFromUri(this, noteId, it) }
        if (paths.isEmpty()) return
        store.updateNote(noteId) { it.copy(imagePaths = it.imagePaths + paths) }
        StickyOverlay.sync(this)
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"

        fun intent(context: Context, noteId: String): Intent =
            Intent(context, StickyNotePickActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, noteId)
    }
}
