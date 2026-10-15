package com.madlauncher.a11y

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Type a contact name into an app's search and tap the one row with that exact name. */
object MessengerSearchTapEngine {
    enum class Step {
        Progress,
        Completed,
        NotFound,
    }

    fun isExactName(label: String, name: String): Boolean {
        val want = name.trim()
        if (want.isEmpty()) return false
        return label.trim().equals(want, ignoreCase = true)
    }

    fun isOpenChat(label: String): Boolean {
        val hay = label.trim().lowercase()
        return hay.contains("thread details") ||
            hay == "type a message" ||
            hay == "write a message" ||
            hay == "כתוב הודעה"
    }

    fun isPersonRow(label: String, name: String): Boolean {
        val head = label.substringBefore(",").substringBefore("·").trim()
        if (head.contains(" and ", ignoreCase = true)) return false
        val want = letters(name)
        val got = letters(head)
        return want.length >= 2 && got == want
    }

    fun tokensMatch(query: String, label: String): Boolean {
        val head = label.substringBefore(",").substringBefore("·").trim()
        if (head.contains(" and ", ignoreCase = true)) return false
        val want = words(query)
        val got = words(head)
        if (want.isEmpty() || got.isEmpty()) return false
        return want.all { word -> got.any { editDistance(word, it) <= 1 } }
    }

    private fun letters(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

    private fun words(value: String): List<String> =
        value.lowercase()
            .replace(Regex("['’ʼ`׳´]"), "")
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }

    private fun editDistance(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            for (j in 0..b.length) prev[j] = cur[j]
        }
        return prev[b.length]
    }

    fun isSearchLabel(label: String): Boolean {
        val hay = label.trim().lowercase()
        if (hay.contains("setting")) return false
        return hay == "search" || hay == "חיפוש" || hay.startsWith("search ")
    }

    fun isComposerLabel(label: String): Boolean {
        val hay = label.trim().lowercase()
        return hay == "new message" || hay == "הודעה חדשה"
    }

    fun attempt(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: MessengerSearchBridge.Request,
    ): Step {
        if (request.picked && findSearchField(root) == null && chatIsOpen(root)) {
            request.phase = MessengerSearchBridge.Phase.Done
            return Step.Completed
        }
        return when (request.phase) {
            MessengerSearchBridge.Phase.OpenSearch -> openSearch(service, root, request)
            MessengerSearchBridge.Phase.TypeQuery -> typeQuery(root, request)
            MessengerSearchBridge.Phase.PickResult -> pickResult(service, root, request)
            MessengerSearchBridge.Phase.Done -> Step.Completed
        }
    }

    private fun openSearch(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: MessengerSearchBridge.Request,
    ): Step {
        if (findSearchField(root) != null) {
            request.phase = MessengerSearchBridge.Phase.TypeQuery
            return Step.Progress
        }
        val existing = exactContactRows(root, request.contactName).minByOrNull { topOf(it) }
        if (existing != null) {
            if (!OttCallTapEngine.performClick(service, existing)) return Step.NotFound
            request.picked = true
            return Step.Progress
        }
        val search = findSearchButton(root) ?: return Step.NotFound
        if (!OttCallTapEngine.performClick(service, search)) return Step.NotFound
        request.phase = MessengerSearchBridge.Phase.TypeQuery
        return Step.Progress
    }

    private fun typeQuery(root: AccessibilityNodeInfo, request: MessengerSearchBridge.Request): Step {
        val field = findSearchField(root) ?: return Step.NotFound
        val query = currentQuery(request)
        val current = field.text?.toString().orEmpty()
        if (letters(current) == letters(query) && letters(query).isNotEmpty()) {
            request.phase = MessengerSearchBridge.Phase.PickResult
            return Step.Progress
        }
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                query,
            )
        }
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return Step.NotFound
        request.phase = MessengerSearchBridge.Phase.PickResult
        return Step.Progress
    }

    private fun pickResult(
        service: AccessibilityService,
        root: AccessibilityNodeInfo,
        request: MessengerSearchBridge.Request,
    ): Step {
        if (request.picked) return Step.NotFound
        val hits = exactContactRows(root, request.contactName)
        val chosen = hits.minByOrNull { topOf(it) }
            ?: tokenContactRows(root, request.contactName).minByOrNull { topOf(it) }
        if (chosen == null) {
            if (request.queryIndex < request.queries.lastIndex) {
                request.queryIndex += 1
                request.phase = MessengerSearchBridge.Phase.TypeQuery
                return Step.Progress
            }
            return Step.NotFound
        }
        if (!OttCallTapEngine.performClick(service, chosen)) return Step.NotFound
        request.picked = true
        return Step.Progress
    }

    private fun topOf(node: AccessibilityNodeInfo): Int {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return rect.top
    }

    private fun chatIsOpen(root: AccessibilityNodeInfo): Boolean = anyLabel(root) { isOpenChat(it) }

    private fun findSearchField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return firstNode(root) { node ->
            val cls = node.className?.toString().orEmpty()
            cls.contains("AutoCompleteTextView") ||
                (isTextField(node) && labelOf(node).lowercase().contains("search"))
        }
    }

    private fun exactContactRows(root: AccessibilityNodeInfo, name: String): List<AccessibilityNodeInfo> {
        val seen = linkedSetOf<String>()
        val exact = mutableListOf<AccessibilityNodeInfo>()
        val prefixed = mutableListOf<AccessibilityNodeInfo>()
        walk(root) { node ->
            if (isTextField(node) || node.isEditable) return@walk
            val label = node.text?.toString().orEmpty().ifBlank { node.contentDescription?.toString().orEmpty() }
            if (!isPersonRow(label, name)) return@walk
            val target = clickable(node) ?: return@walk
            if (isTextField(target) || target.isEditable) return@walk
            val key = boundsKey(target)
            if (!seen.add(key)) return@walk
            if (isExactName(label, name)) exact += target else prefixed += target
        }
        return exact.ifEmpty { prefixed }
    }

    private fun currentQuery(request: MessengerSearchBridge.Request): String =
        request.queries.getOrElse(request.queryIndex) { request.contactName }

    private fun tokenContactRows(root: AccessibilityNodeInfo, name: String): List<AccessibilityNodeInfo> {
        val seen = linkedSetOf<String>()
        val hits = mutableListOf<AccessibilityNodeInfo>()
        walk(root) { node ->
            if (isTextField(node) || node.isEditable) return@walk
            val label = node.text?.toString().orEmpty().ifBlank { node.contentDescription?.toString().orEmpty() }
            if (isChromeLabel(label) || !tokensMatch(name, label)) return@walk
            val target = clickable(node) ?: return@walk
            if (isTextField(target) || target.isEditable) return@walk
            if (seen.add(boundsKey(target))) hits += target
        }
        return hits
    }

    private fun isChromeLabel(label: String): Boolean {
        val hay = label.trim().lowercase()
        return hay == "back" || hay == "to:" || hay == "new message" || hay == "more people" ||
            hay == "group chat" || hay == "suggested" || hay.startsWith("type a name") ||
            hay.startsWith("no results")
    }

    private fun findSearchButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var found: AccessibilityNodeInfo? = null
        walk(root) { node ->
            if (found != null || isTextField(node) || node.isEditable) return@walk
            val label = node.text?.toString().orEmpty().ifBlank { node.contentDescription?.toString().orEmpty() }
            if (isSearchLabel(label) || isComposerLabel(label)) found = clickable(node)
        }
        return found
    }

    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) return current
            current = current.parent
        }
        return null
    }

    private fun isTextField(node: AccessibilityNodeInfo): Boolean {
        val cls = node.className?.toString().orEmpty()
        return cls.contains("EditText") || cls.contains("AutoCompleteTextView")
    }

    private fun labelOf(node: AccessibilityNodeInfo): String = buildString {
        append(node.text?.toString().orEmpty())
        append(' ')
        append(node.contentDescription?.toString().orEmpty())
        append(' ')
        append(node.hintText?.toString().orEmpty())
    }

    private fun boundsKey(node: AccessibilityNodeInfo): String {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return "${rect.left},${rect.top},${rect.right},${rect.bottom}"
    }

    private fun anyLabel(root: AccessibilityNodeInfo, match: (String) -> Boolean): Boolean {
        var found = false
        walk(root) { node ->
            if (found) return@walk
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            if (match(text) || match(desc)) found = true
        }
        return found
    }

    private fun firstNode(
        root: AccessibilityNodeInfo,
        match: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        var found: AccessibilityNodeInfo? = null
        walk(root) { node ->
            if (found == null && match(node)) found = node
        }
        return found
    }

    private fun walk(root: AccessibilityNodeInfo, visit: (AccessibilityNodeInfo) -> Unit) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            visit(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }
}
