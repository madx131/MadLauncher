package com.madlauncher

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.madlauncher.ui.ActionBoxOverlay
import com.madlauncher.ui.LanguageOverlayContextWrapper
import com.madlauncher.data.LanguageOverlay

/**
 * Translucent Action Box over whatever home is showing. Not [MainActivity] —
 * that is HOME/singleTask and would replace the other launcher.
 */
class ActionBoxActivity : ComponentActivity() {
    companion object {
        const val ACTION_OPEN = "com.madlauncher.action.OPEN_ACTION_BOX"

        fun intent(context: Context): Intent =
            Intent(context, ActionBoxActivity::class.java).setAction(ACTION_OPEN)
    }

    private var overlayResources: Resources? = null
    private var overlayResourcesConfigHash: Int = 0
    private var openedAtMs = 0L

    override fun getResources(): Resources {
        val base = super.getResources()
        val hash = base.configuration.hashCode()
        if (overlayResources == null || overlayResourcesConfigHash != hash) {
            overlayResources = LanguageOverlayContextWrapper.OverlayResources(base)
            overlayResourcesConfigHash = hash
        }
        return overlayResources!!
    }

    override fun attachBaseContext(newBase: Context) {
        if (BuildConfig.ACTION_BOX_APP) {
            super.attachBaseContext(newBase)
        } else {
            super.attachBaseContext(MadApp.englishContext(newBase))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.ACTION_BOX_APP) {
            LanguageOverlay.applySystemLanguage(this)
        }
        applyWindow()
        openedAtMs = SystemClock.uptimeMillis()
        if (BuildConfig.ACTION_BOX_APP) {
            LauncherWindowDismiss.preserve(1_200L)
        }
        overridePendingTransition(R.anim.action_box_enter, 0)
        setContent {
            ActionBoxOverlay(onFinished = { finishOverlay() })
        }
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        try {
            super.startActivity(intent, options)
        } catch (e: SecurityException) {
            if (intent.action?.contains("ACCESSIBILITY", ignoreCase = true) == true) return
            throw e
        }
    }

    @Deprecated("Framework still funnels Activity Result launches through this.")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        try {
            @Suppress("DEPRECATION")
            super.startActivityForResult(intent, requestCode, options)
        } catch (e: SecurityException) {
            if (intent.action?.contains("ACCESSIBILITY", ignoreCase = true) == true) return
            throw e
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (LauncherWindowDismiss.shouldPreserve()) return
        // Over another home (Microsoft), IME and that launcher fire this while
        // the field should stay. Close from onStop / scrim / Back instead.
        if (BuildConfig.ACTION_BOX_APP) return
        finishOverlay()
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        if (LauncherWindowDismiss.shouldPreserve()) return
        if (BuildConfig.ACTION_BOX_APP) {
            if (SystemClock.uptimeMillis() - openedAtMs < 1_200L) return
            if (imeVisible()) return
        }
        finishOverlay()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.action_box_exit)
    }

    private fun finishOverlay() {
        if (isFinishing) return
        finish()
    }

    private fun imeVisible(): Boolean {
        val raw = window.decorView.rootWindowInsets ?: return false
        return WindowInsetsCompat.toWindowInsetsCompat(raw)
            .isVisible(WindowInsetsCompat.Type.ime())
    }

    private fun applyWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
        )
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        val dark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
