package com.madlauncher.a11y

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/** Opens a social app's compose screen and inserts post text via accessibility. */
object OttSocialPostBridge {
    enum class Phase {
        OpenApp,
        TapCompose,
        InsertText,
        Done,
    }

    class Request(
        val packageName: String,
        val text: String,
        val deadlineMs: Long = System.currentTimeMillis() + 28_000L,
        var phase: Phase = Phase.OpenApp,
        var relaunchCount: Int = 0,
        var lastRelaunchMs: Long = 0L,
        var tapCount: Int = 0,
        var gestureTried: Boolean = false,
        var homeTried: Boolean = false,
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    var pending: Request? = null
        private set

    fun begin(context: Context, packageName: String, text: String, launchApp: Boolean = true) {
        if (text.isNotBlank()) {
            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clip?.setPrimaryClip(android.content.ClipData.newPlainText("Post", text))
        }
        val meta = packageName.lowercase().let { it.contains("facebook") || it.contains("instagram") }
        if (!launchApp) {
            pending = Request(packageName = packageName, text = text, phase = Phase.TapCompose)
            MadAccessibilityService.instance?.scheduleSocialPostAttempt(700L)
            return
        }
        val launch = bringToFront(context, packageName) ?: return
        pending = Request(packageName = packageName, text = text)
        // A second MAIN warm-start dumps Facebook on Reels.
        if (!meta) warmProcess(context, packageName)
        mainHandler.postDelayed({
            pending?.phase = Phase.TapCompose
            runCatching { context.startActivity(Intent(launch)) }
            MadAccessibilityService.instance?.scheduleSocialPostAttempt(if (meta) 1_100L else 900L)
        }, if (meta) 180L else 420L)
        MadAccessibilityService.instance?.scheduleSocialPostAttempt(if (meta) 1_800L else 1_400L)
    }

    fun clear() {
        pending = null
    }

    fun markDone() {
        pending = null
    }

    fun maybeRelaunch(context: Context, req: Request): Boolean {
        val pkg = req.packageName.lowercase()
        // MAIN relaunch dumps Facebook on Reels / Instagram on feed with no composer.
        if (pkg.contains("facebook") || pkg.contains("instagram")) return false
        val now = System.currentTimeMillis()
        if (req.relaunchCount >= 3) return false
        if (now - req.lastRelaunchMs < 1_500L) return false
        req.relaunchCount += 1
        req.lastRelaunchMs = now
        val launch = bringToFront(context, req.packageName) ?: return false
        mainHandler.postDelayed({
            runCatching { context.startActivity(launch) }
        }, 200L)
        return true
    }

    private fun warmProcess(context: Context, packageName: String) {
        val launch = bringToFront(context, packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching { context.startActivity(launch) }
    }

    /** Reorder only — CLEAR_TOP dumps Facebook on Reels, which has no status composer. */
    private fun bringToFront(context: Context, packageName: String): Intent? {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        return launch
    }
}
