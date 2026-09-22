package com.madlauncher.a11y

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.madlauncher.data.PermissionChecker
import java.util.concurrent.ConcurrentHashMap

/**
 * After a generic ACTION_SEND, look once for a composer that actually received the text.
 * Remember the outcome so later `!` follow the real channel.
 */
object OttSocialSendLearn {
    private const val TIMEOUT_MS = 3_500L

    class Watch(
        val appContext: Context,
        val packageName: String,
        val text: String,
        val deadlineMs: Long = System.currentTimeMillis() + TIMEOUT_MS,
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val timeoutRunnable = Runnable { finish(openedComposer = false) }
    private val sendOutcome = ConcurrentHashMap<String, Boolean>()

    @Volatile
    var pending: Watch? = null
        private set

    fun socialSendOutcome(packageName: String): Boolean? = sendOutcome[packageName.lowercase()]

    fun watch(context: Context, packageName: String, text: String) {
        if (socialSendOutcome(packageName) != null) return
        if (!PermissionChecker.isAccessibilityEnabled(context)) return
        pending = Watch(
            appContext = context.applicationContext,
            packageName = packageName,
            text = text,
        )
        mainHandler.removeCallbacks(timeoutRunnable)
        MadAccessibilityService.instance?.scheduleSocialSendLearn(400L)
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)
    }

    fun onTree(root: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        val watch = pending ?: return false
        if (!OttSocialPostTapEngine.sendOpenedComposer(root, watch.text)) return false
        finish(openedComposer = true)
        return true
    }

    fun clear() {
        mainHandler.removeCallbacks(timeoutRunnable)
        pending = null
    }

    private fun finish(openedComposer: Boolean) {
        val watch = pending ?: return
        mainHandler.removeCallbacks(timeoutRunnable)
        pending = null
        sendOutcome[watch.packageName.lowercase()] = openedComposer
        if (!openedComposer) {
            OttSocialPostBridge.begin(watch.appContext, watch.packageName, watch.text)
        }
    }
}
