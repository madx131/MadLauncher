package com.madlauncher.a11y

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/** Pending `#` / `*` call — warm app, open thread, tap in-app call via [MadAccessibilityService]. */
object OttCallBridge {
    enum class Phase {
        PickContact,
        WaitingForUi,
        Toolbar,
        Confirm,
        Permission,
        Done,
    }

    class Request(
        val packageName: String,
        val video: Boolean,
        val deadlineMs: Long = System.currentTimeMillis() + 22_000L,
        var phase: Phase = Phase.WaitingForUi,
        var relaunchIntent: Intent? = null,
        var relaunchCount: Int = 0,
        var lastRelaunchMs: Long = 0L,
        var contactName: String? = null,
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var stashedRelaunch: Intent? = null

    @Volatile
    var pending: Request? = null
        private set

    fun stashRelaunch(intent: Intent) {
        stashedRelaunch = Intent(intent)
    }

    private fun takeStashedRelaunch(): Intent? = stashedRelaunch?.also { stashedRelaunch = null }

    fun begin(
        context: Context,
        packageName: String,
        video: Boolean,
        threadIntent: Intent,
    ) {
        val launch = Intent(threadIntent).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        pending = Request(
            packageName = packageName,
            video = video,
            relaunchIntent = Intent(launch),
        )
        warmProcess(context, packageName)
        mainHandler.postDelayed({
            runCatching { context.startActivity(Intent(launch)) }
            MadAccessibilityService.instance?.scheduleOttCallAttempt(700L)
        }, 380L)
        MadAccessibilityService.instance?.scheduleOttCallAttempt(1_200L)
    }

    fun request(context: Context, packageName: String, video: Boolean, relaunchIntent: Intent? = null) {
        pending = Request(
            packageName = packageName,
            video = video,
            relaunchIntent = relaunchIntent?.let { Intent(it) } ?: takeStashedRelaunch(),
        )
        MadAccessibilityService.instance?.scheduleOttCallAttempt(900L)
    }

    fun maybeRelaunch(context: Context, req: Request): Boolean {
        val intent = req.relaunchIntent ?: return false
        val now = System.currentTimeMillis()
        if (req.relaunchCount >= 4) return false
        if (now - req.lastRelaunchMs < 1_800L) return false
        req.relaunchCount += 1
        req.lastRelaunchMs = now
        warmProcess(context, req.packageName)
        mainHandler.postDelayed({
            runCatching {
                context.startActivity(
                    Intent(intent).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                        )
                    },
                )
            }
        }, 200L)
        return true
    }

    private fun warmProcess(context: Context, packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching { context.startActivity(launch) }
    }

    fun clear() {
        pending = null
    }

    internal fun markDone() {
        pending = null
    }
}
