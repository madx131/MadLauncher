package com.madlauncher.a11y

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Find and tap OTT in-app call controls (WhatsApp, Messenger, …). */
object OttCallTapEngine {
    enum class Step {
        Progress,
        Completed,
        NotFound,
    }

    private val entryViewIds = listOf(
        "com.whatsapp:id/entry",
        "com.whatsapp.w4b:id/entry",
    )
    private val voiceViewIds = listOf(
        "com.whatsapp:id/menuitem_voice_call",
        "com.whatsapp:id/voice_call_btn",
        "com.whatsapp:id/conversation_contact_status",
        "com.whatsapp.w4b:id/menuitem_voice_call",
        "com.whatsapp.w4b:id/voice_call_btn",
    )
    private val videoViewIds = listOf(
        "com.whatsapp:id/menuitem_video_call",
        "com.whatsapp:id/video_call_btn",
        "com.whatsapp.w4b:id/menuitem_video_call",
        "com.whatsapp.w4b:id/video_call_btn",
    )

    fun attempt(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttCallBridge.Request,
    ): Step {
        return when (request.phase) {
            OttCallBridge.Phase.PickContact -> tapContactRow(service, root, request)
            OttCallBridge.Phase.WaitingForUi -> waitForConversation(root, request)
            OttCallBridge.Phase.Toolbar -> tapToolbar(service, root, request)
            OttCallBridge.Phase.Confirm -> tapConfirm(service, root, request)
            OttCallBridge.Phase.Permission -> tapPermission(service, root, request)
            OttCallBridge.Phase.Done -> Step.Completed
        }
    }

    internal fun isConversationReady(root: AccessibilityNodeInfo): Boolean {
        if (findByViewId(root, entryViewIds) != null) return true
        if (findByViewId(root, voiceViewIds + videoViewIds) != null) return true
        return findClickableByHint(root, listOf("voice call", "video call", "type a message")) != null
    }

    private fun tapContactRow(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttCallBridge.Request,
    ): Step {
        val name = request.contactName?.trim().orEmpty()
        if (name.isBlank()) {
            request.phase = OttCallBridge.Phase.WaitingForUi
            return Step.NotFound
        }
        val node = findClickableByExactText(root, name)
            ?: findClickableContainingText(root, name)
        if (node == null) return Step.NotFound
        if (!performClick(service, node)) return Step.NotFound
        request.phase = OttCallBridge.Phase.WaitingForUi
        return Step.Progress
    }

    private fun findClickableContainingText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val want = text.lowercase()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val label = node.text?.toString()?.trim()?.lowercase().orEmpty()
            if (label.contains(want)) {
                clickableTarget(node)?.let { return it }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun waitForConversation(root: AccessibilityNodeInfo, request: OttCallBridge.Request): Step {
        if (!isConversationReady(root)) return Step.NotFound
        request.phase = OttCallBridge.Phase.Toolbar
        return Step.Progress
    }

    private fun tapToolbar(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttCallBridge.Request,
    ): Step {
        val ids = if (request.video) videoViewIds else voiceViewIds
        val hints = if (request.video) {
            listOf("video call")
        } else {
            listOf("voice call", "audio call")
        }
        val node = findByViewId(root, ids) ?: findClickableByHint(root, hints)
        if (node == null) return Step.NotFound
        if (!performClick(service, node)) return Step.NotFound
        request.phase = OttCallBridge.Phase.Confirm
        return Step.Progress
    }

    private fun tapConfirm(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttCallBridge.Request,
    ): Step {
        val node = findClickableByExactText(root, "Call")
            ?: findClickableByHint(root, listOf("start voice call", "start video call"))
        if (node == null) {
            if (looksLikeInCallScreen(root)) {
                request.phase = OttCallBridge.Phase.Done
                return Step.Completed
            }
            return Step.NotFound
        }
        if (!performClick(service, node)) return Step.NotFound
        request.phase = OttCallBridge.Phase.Permission
        return Step.Progress
    }

    private fun tapPermission(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttCallBridge.Request,
    ): Step {
        val node = findClickableByExactText(root, "Allow")
            ?: findClickableByHint(root, listOf("allow", "while using the app"))
        if (node != null) {
            performClick(service, node)
        }
        request.phase = OttCallBridge.Phase.Done
        return Step.Completed
    }

    private fun looksLikeInCallScreen(root: AccessibilityNodeInfo): Boolean {
        return findClickableByHint(
            root,
            listOf("end call", "leave call", "mute", "speaker"),
        ) != null
    }

    internal fun findByViewId(root: AccessibilityNodeInfo, ids: List<String>): AccessibilityNodeInfo? {
        for (id in ids) {
            val found = root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()
            if (found != null) return found
        }
        return null
    }

    internal fun findClickableByHint(root: AccessibilityNodeInfo, hints: List<String>): AccessibilityNodeInfo? {
        val normalized = hints.map { normalizeHint(it) }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val haystack = normalizeHint(
                buildString {
                    append(node.contentDescription?.toString().orEmpty())
                    append(' ')
                    append(node.text?.toString().orEmpty())
                    append(' ')
                    append(node.hintText?.toString().orEmpty())
                },
            )
            if (normalized.any { haystack.contains(it) }) {
                clickableTarget(node)?.let { return it }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    internal fun findClickableByExactText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val want = text.lowercase()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val label = node.text?.toString()?.trim()?.lowercase().orEmpty()
            if (label == want) {
                clickableTarget(node)?.let { return it }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) return current
            current = current.parent
        }
        return if (node.isEnabled) node else null
    }

    internal fun performClick(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        val target = clickableTarget(node) ?: return false
        if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        if (target.performAction(AccessibilityNodeInfo.ACTION_FOCUS) &&
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        ) {
            return true
        }
        return dispatchTap(service, target)
    }

    internal fun dispatchTapAt(service: AccessibilityService, x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        if (x <= 0f || y <= 0f) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 48)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun dispatchTap(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return false
        return dispatchTapAt(service, rect.exactCenterX(), rect.exactCenterY())
    }

    private fun normalizeHint(value: String): String =
        value.lowercase().replace('’', '\'').replace('‘', '\'')
}
