package com.madlauncher

import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.madlauncher.data.WallpaperApplier

/** Receives "set as wallpaper" from Photos, wallpaper apps, and crop-and-set intents. */
class SetWallpaperActivity : ComponentActivity() {
    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Toast.makeText(
            this,
            if (result.resultCode == RESULT_OK) "Wallpaper updated" else "Wallpaper crop cancelled",
            Toast.LENGTH_SHORT,
        ).show()
        setResult(result.resultCode)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = WallpaperApplier.uriFrom(intent)
        if (uri != null) {
            val crop = WallpaperApplier.cropAndSetIntent(this, uri)
            if (crop != null) {
                runCatching { cropLauncher.launch(crop) }
                    .onFailure { applyDirect(uri) }
                return
            }
            applyDirect(uri)
            finish()
            return
        }
        runCatching {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }.onFailure {
            Toast.makeText(this, "Couldn't open wallpaper picker", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun applyDirect(uri: android.net.Uri) {
        val ok = WallpaperApplier.applyImage(this, uri)
        Toast.makeText(
            this,
            if (ok) "Wallpaper updated" else "Couldn't set wallpaper",
            Toast.LENGTH_SHORT,
        ).show()
        setResult(if (ok) RESULT_OK else RESULT_CANCELED)
    }
}
