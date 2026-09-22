package com.madlauncher.a11y

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Paste Action Box text into an open chat composer. Never taps Send. */
object OttChatSendTapEngine {
    enum class Step {
        Progress,
        Completed,
        NotFound,
    }

    private val entryIds = listOf(
        "com.whatsapp:id/entry",
        "com.whatsapp.w4b:id/entry",
        "org.telegram.messenger:id/chat_message_edit",
        "org.telegram.messenger.web:id/chat_message_edit",
        "org.telegram.messenger:id/chat_text_enter",
        "org.telegram.messenger.web:id/chat_text_enter",
    )

    private val entryHints = listOf(
        "type a message",
        "message",
        "write a message",
        "כתוב הודעה",
        "הודעה",
    )

    fun attempt(
        @Suppress("UNUSED_PARAMETER") service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttChatSendBridge.Request,
    ): Step {
        return when (request.phase) {
            OttChatSendBridge.Phase.WaitForChat -> waitForChat(root, request)
            OttChatSendBridge.Phase.InsertText -> insertText(root, request)
            OttChatSendBridge.Phase.Done -> Step.Completed
        }
    }

    private fun waitForChat(root: AccessibilityNodeInfo, request: OttChatSendBridge.Request): Step {
        val field = findEntry(root) ?: return Step.NotFound
        if (request.alreadyFilled || fieldAlreadyHas(field, request.text)) {
            request.phase = OttChatSendBridge.Phase.Done
            return Step.Completed
        }
        request.phase = OttChatSendBridge.Phase.InsertText
        return Step.Progress
    }

    private fun insertText(root: AccessibilityNodeInfo, request: OttChatSendBridge.Request): Step {
        val field = findEntry(root) ?: return Step.NotFound
        if (fieldAlreadyHas(field, request.text)) {
            request.phase = OttChatSendBridge.Phase.Done
            return Step.Completed
        }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, request.text)
        }
        val set = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        val pasted = if (!set && field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } else {
            set
        }
        if (!pasted) return Step.NotFound
        request.phase = OttChatSendBridge.Phase.Done
        return Step.Completed
    }

    private fun findEntry(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        OttCallTapEngine.findByViewId(root, entryIds)?.let { return it }
        findEditableByHint(root, entryHints)?.let { return it }
        return findChatEditable(root)
    }

    private fun findEditableByHint(root: AccessibilityNodeInfo, hints: List<String>): AccessibilityNodeInfo? {
        val want = hints.map { it.lowercase() }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (looksLikeChatField(node)) {
                val hay = fieldHaystack(node)
                if (want.any { hay.contains(it) } && !hay.contains("search")) return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findChatEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (looksLikeChatField(node) && !fieldHaystack(node).contains("search")) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun looksLikeChatField(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled || !node.isVisibleToUser) return false
        return node.isEditable || node.className?.toString().orEmpty().contains("EditText")
    }

    private fun fieldAlreadyHas(node: AccessibilityNodeInfo, text: String): Boolean {
        val snippet = text.trim().take(24).lowercase()
        if (snippet.length < 2) return false
        return fieldHaystack(node).contains(snippet)
    }

    private fun fieldHaystack(node: AccessibilityNodeInfo): String =
        buildString {
            append(node.hintText?.toString().orEmpty())
            append(' ')
            append(node.contentDescription?.toString().orEmpty())
            append(' ')
            append(node.text?.toString().orEmpty())
        }.lowercase()
}
