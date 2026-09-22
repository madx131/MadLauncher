package com.madlauncher.a11y

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/**
 * After `@` opens a chat that didn't receive the Action Box text (Telegram),
 * insert it into the composer. Never taps Send.
 */
object OttChatSendBridge {
    enum class Phase {
        WaitForChat,
        InsertText,
        Done,
    }

    class Request(
        val packageName: String,
        val text: String,
        val alreadyFilled: Boolean,
        val deadlineMs: Long = System.currentTimeMillis() + 16_000L,
        var phase: Phase = Phase.WaitForChat,
        var relaunchIntent: Intent? = null,
        var relaunchCount: Int = 0,
        var lastRelaunchMs: Long = 0L,
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    var pending: Request? = null
        private set

    fun begin(
        context: Context,
        packageName: String,
        text: String,
        alreadyFilled: Boolean,
        chatIntent: Intent?,
    ) {
        pending = Request(
            packageName = packageName,
            text = text,
            alreadyFilled = alreadyFilled,
            relaunchIntent = chatIntent?.let { Intent(it) },
        )
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clip?.setPrimaryClip(android.content.ClipData.newPlainText("Message", text))
        MadAccessibilityService.instance?.scheduleChatSendAttempt(700L)
        MadAccessibilityService.instance?.scheduleChatSendAttempt(1_400L)
    }

    fun maybeRelaunch(context: Context, req: Request): Boolean {
        val intent = req.relaunchIntent ?: return false
        val now = System.currentTimeMillis()
        if (req.relaunchCount >= 3) return false
        if (now - req.lastRelaunchMs < 1_600L) return false
        req.relaunchCount += 1
        req.lastRelaunchMs = now
        mainHandler.postDelayed({
            runCatching {
                context.startActivity(
                    Intent(intent).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        )
                    },
                )
            }
        }, 200L)
        return true
    }

    fun clear() {
        pending = null
    }

    fun markDone() {
        pending = null
    }
}
