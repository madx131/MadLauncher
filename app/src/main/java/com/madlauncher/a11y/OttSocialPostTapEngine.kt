package com.madlauncher.a11y

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object OttSocialPostTapEngine {
    enum class Step {
        Progress,
        Completed,
        NotFound,
    }

    private val facebookComposerIds = listOf(
        "com.facebook.katana:id/composer_hint_view",
        "com.facebook.katana:id/status_text",
        "com.facebook.katana:id/feed_composer",
        "com.facebook.katana:id/mentions_composer_text",
        "com.facebook.katana:id/feed_composer_text",
        "com.facebook.lite:id/composer_hint_view",
    )

    private val facebookHomeIds = listOf(
        "com.facebook.katana:id/feed_tab",
        "com.facebook.katana:id/tab_feed",
        "com.facebook.katana:id/bottom_nav_home",
        "com.facebook.katana:id/navigation_bar_item_home",
        "com.facebook.lite:id/feed_tab",
    )

    private val facebookHints = listOf(
        "what's on your mind",
        "on your mind",
        "what's going on",
        "make a post",
        "create a post",
        "write a post",
        "start a post",
        "share something",
        "על מה אתה חושב",
        "על מה את חושבת",
        "במה אתה חושב",
    )

    private val instagramCreateIds = listOf(
        "com.instagram.android:id/creation_tab",
        "com.instagram.android:id/tab_creation",
        "com.instagram.android:id/creation_tab_icon",
    )

    private val instagramCreateHints = listOf(
        "create",
        "new post",
        "add post",
        "camera",
        "plus",
    )

    private val genericComposeHints = listOf(
        "new post",
        "what's on your mind",
        "what's happening",
        "write something",
        "start a post",
        "compose",
        "share update",
    )

    private val genericComposeIds = listOf(
        "com.twitter.android:id/composer_write",
    )

    private val textFieldIds = listOf(
        "com.facebook.katana:id/status_text",
        "com.facebook.katana:id/mentions_composer_text",
        "com.facebook.katana:id/composer_status_text",
        "com.twitter.android:id/tweet_text",
        "com.twitter.android:id/composer_edit_text",
        "com.instagram.android:id/caption_input_text_view",
        "com.instagram.android:id/caption_text_view",
        "com.instagram.android:id/caption_input_text_view",
        "com.linkedin.android:id/share_compose_text_input",
        "com.reddit.frontpage:id/submit_text_field",
    )

    fun attempt(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): Step {
        return when (request.phase) {
            OttSocialPostBridge.Phase.OpenApp -> {
                request.phase = OttSocialPostBridge.Phase.TapCompose
                Step.Progress
            }
            OttSocialPostBridge.Phase.TapCompose -> tapCompose(service, root, request)
            OttSocialPostBridge.Phase.InsertText -> insertText(root, request)
            OttSocialPostBridge.Phase.Done -> Step.Completed
        }
    }

    private fun tapCompose(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): Step {
        val pkg = request.packageName.lowercase()
        val field = findComposerField(root)
        if (field != null) {
            if (request.text.isBlank()) {
                field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                field.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                request.phase = OttSocialPostBridge.Phase.Done
                return Step.Completed
            }
            request.phase = OttSocialPostBridge.Phase.InsertText
            return Step.Progress
        }
        return when {
            pkg.contains("facebook") -> tapFacebook(service, root, request)
            pkg.contains("instagram") -> tapInstagram(service, root, request)
            else -> tapGeneric(service, root, request)
        }
    }

    private fun tapFacebook(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): Step {
        for (id in facebookComposerIds) {
            val node = OttCallTapEngine.findByViewId(root, listOf(id))
            if (node != null && OttCallTapEngine.performClick(service, node)) {
                return afterComposerControlTap(request)
            }
        }
        val hint = OttCallTapEngine.findClickableByHint(root, facebookHints)
        if (hint != null && OttCallTapEngine.performClick(service, hint)) {
            return afterComposerControlTap(request)
        }
        // Reels / Watch has no status field — leave that tab before tapping the composer.
        if (!request.homeTried) {
            request.homeTried = true
            val home = OttCallTapEngine.findByViewId(root, facebookHomeIds)
                ?: OttCallTapEngine.findClickableByExactText(root, "Home")
                ?: OttCallTapEngine.findClickableByExactText(root, "ראשי")
            if (home != null) {
                OttCallTapEngine.performClick(service, home)
            } else {
                val dm = service.resources.displayMetrics
                OttCallTapEngine.dispatchTapAt(service, dm.widthPixels * 0.12f, dm.heightPixels * 0.94f)
            }
            request.tapCount += 1
            return Step.Progress
        }
        if (!request.gestureTried) {
            request.gestureTried = true
            val dm = service.resources.displayMetrics
            OttCallTapEngine.dispatchTapAt(service, dm.widthPixels * 0.28f, dm.heightPixels * 0.18f)
            request.tapCount += 1
            return Step.Progress
        }
        return Step.NotFound
    }

    private fun afterComposerControlTap(request: OttSocialPostBridge.Request): Step {
        request.tapCount += 1
        if (request.text.isBlank()) return Step.Progress
        request.phase = OttSocialPostBridge.Phase.InsertText
        return Step.Progress
    }

    private fun tapInstagram(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): Step {
        when (request.tapCount) {
            0 -> {
                for (id in instagramCreateIds) {
                    val node = OttCallTapEngine.findByViewId(root, listOf(id))
                    if (node != null && OttCallTapEngine.performClick(service, node)) {
                        request.tapCount = 1
                        return Step.Progress
                    }
                }
                val create = OttCallTapEngine.findClickableByExactText(root, "Create")
                    ?: OttCallTapEngine.findClickableByExactText(root, "Post")
                    ?: OttCallTapEngine.findClickableByHint(root, instagramCreateHints)
                if (create != null && OttCallTapEngine.performClick(service, create)) {
                    request.tapCount = 1
                    return Step.Progress
                }
                if (!request.gestureTried) {
                    request.gestureTried = true
                    val dm = service.resources.displayMetrics
                    OttCallTapEngine.dispatchTapAt(service, dm.widthPixels * 0.5f, dm.heightPixels * 0.94f)
                    request.tapCount = 1
                    return Step.Progress
                }
                return Step.NotFound
            }
            1 -> {
                val post = OttCallTapEngine.findClickableByExactText(root, "POST")
                    ?: OttCallTapEngine.findClickableByExactText(root, "Post")
                    ?: OttCallTapEngine.findClickableByExactText(root, "פוסט")
                if (post != null && OttCallTapEngine.performClick(service, post)) {
                    request.tapCount = 2
                    return Step.Progress
                }
                if (findTextField(root) != null) {
                    request.phase = OttSocialPostBridge.Phase.InsertText
                    return Step.Progress
                }
                return Step.NotFound
            }
            else -> {
                val next = OttCallTapEngine.findClickableByExactText(root, "Next")
                    ?: OttCallTapEngine.findClickableByHint(root, listOf("next", "ok"))
                if (next != null && OttCallTapEngine.performClick(service, next)) {
                    request.phase = OttSocialPostBridge.Phase.InsertText
                    return Step.Progress
                }
                if (findTextField(root) != null) {
                    request.phase = OttSocialPostBridge.Phase.InsertText
                    return Step.Progress
                }
                return Step.NotFound
            }
        }
    }

    private fun tapGeneric(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): Step {
        for (id in genericComposeIds) {
            val node = OttCallTapEngine.findByViewId(root, listOf(id))
            if (node != null && OttCallTapEngine.performClick(service, node)) {
                request.phase = OttSocialPostBridge.Phase.InsertText
                return Step.Progress
            }
        }
        val compose = OttCallTapEngine.findClickableByHint(root, genericComposeHints)
        if (compose != null && OttCallTapEngine.performClick(service, compose)) {
            request.phase = OttSocialPostBridge.Phase.InsertText
            return Step.Progress
        }
        return Step.NotFound
    }

    private fun insertText(root: AccessibilityNodeInfo, request: OttSocialPostBridge.Request): Step {
        if (request.text.isBlank()) {
            val field = findComposerField(root) ?: findTextField(root)
            if (field != null) {
                field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                field.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                request.phase = OttSocialPostBridge.Phase.Done
                return Step.Completed
            }
            return Step.NotFound
        }
        val field = findPostableField(root, request) ?: return Step.NotFound
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, request.text)
        }
        if (field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            request.phase = OttSocialPostBridge.Phase.Done
            return Step.Completed
        }
        if (field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            request.phase = OttSocialPostBridge.Phase.Done
            return Step.Completed
        }
        return Step.NotFound
    }

    private fun findPostableField(
        root: AccessibilityNodeInfo,
        request: OttSocialPostBridge.Request,
    ): AccessibilityNodeInfo? {
        findComposerField(root)?.let { return it }
        if (request.tapCount > 0 || request.phase == OttSocialPostBridge.Phase.InsertText) {
            return findEditable(root)
        }
        return null
    }

    private fun findComposerField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in textFieldIds) {
            val node = OttCallTapEngine.findByViewId(root, listOf(id)) ?: continue
            if (looksLikeComposerField(node) && isComposerLabeled(node)) return node
        }
        return findLabeledComposer(root)
    }

    private fun findLabeledComposer(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (looksLikeComposerField(node) && isComposerLabeled(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findTextField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in textFieldIds) {
            val node = OttCallTapEngine.findByViewId(root, listOf(id)) ?: continue
            if (looksLikeComposerField(node)) return node
        }
        return findEditable(root)
    }

    private fun findEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (looksLikeComposerField(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun looksLikeComposerField(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled || !node.isVisibleToUser) return false
        val editable = node.isEditable ||
            node.className?.toString().orEmpty().contains("EditText")
        if (!editable) return false
        val hay = fieldHaystack(node)
        if (hay.contains("search") || hay.contains("find friends") || hay.contains("חיפוש")) return false
        return true
    }

    private fun isComposerLabeled(node: AccessibilityNodeInfo): Boolean {
        val hay = fieldHaystack(node)
        val labels = listOf(
            "mind",
            "happening",
            "write a post",
            "create a post",
            "say something",
            "share something",
            "what's going",
            "על מה",
            "במה אתה",
            "כתוב פוסט",
        )
        return labels.any { hay.contains(it) }
    }

    private fun fieldHaystack(node: AccessibilityNodeInfo): String =
        buildString {
            append(node.hintText?.toString().orEmpty())
            append(' ')
            append(node.contentDescription?.toString().orEmpty())
            append(' ')
            append(node.text?.toString().orEmpty())
        }.lowercase().replace('’', '\'').replace('‘', '\'')

    /** True when an editable composer already contains [text] (ACTION_SEND actually landed). */
    fun sendOpenedComposer(root: AccessibilityNodeInfo, text: String): Boolean {
        val snippet = text.trim().take(24).lowercase()
        if (snippet.length < 3) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (looksLikeComposerField(node)) {
                val hay = fieldHaystack(node)
                if (hay.contains(snippet)) return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }
}
