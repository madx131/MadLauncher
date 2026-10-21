package com.madlauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.madlauncher.data.IconStyleStore
import com.madlauncher.data.LocalStore

/**
 * Receives [IconStyleStore.ACTION_INSTALL] from a designer app.
 * The zip URI is [android.content.Intent.EXTRA_STREAM] or the intent data.
 */
class IconStyleInstallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = IconStyleStore.uriFrom(intent)
        val spec = uri?.let { IconStyleStore.import(this, it) }
        if (spec == null) {
            Toast.makeText(this, getString(R.string.settings_icon_style_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val local = LocalStore(this)
        val next = local.appearanceSettings.copy(iconStyleId = spec.id)
        local.appearanceSettings = next
        AppearanceBridge.update(next)
        IconStyleStore.activate(this, spec.id, force = true)
        Toast.makeText(this, getString(R.string.settings_icon_style_installed), Toast.LENGTH_SHORT).show()
        finish()
    }
}
