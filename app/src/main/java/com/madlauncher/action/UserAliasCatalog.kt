package com.madlauncher.action

import org.json.JSONArray
import org.json.JSONObject

/** Built-in Action Box actions that user language-words can map onto. */
enum class AliasTarget {
    Chat,
    Email,
    Social,
    Call,
    Video,
    Translate,
    Notes,
    Sticky,
    Ai,
    Contact,
    Nav,
    Reminder,
    Help,
    AddContact,
    UpdateApps,
    SystemSettings,
    LauncherSettings,
    ActionBoxSettings,
    ;

    fun toCommand(payload: String): ActionCommand = when (this) {
        Chat, Email -> ActionCommand.ChatPick(payload)
        Social -> ActionCommand.PostPick(payload)
        Call -> ActionCommand.Call(payload)
        Video -> ActionCommand.VideoCall(payload)
        Translate -> ActionCommand.Translate(payload)
        Notes -> ActionCommand.NotePick(payload)
        Sticky -> ActionCommand.Note(payload, floating = true)
        Ai -> ActionCommand.AiAsk(payload)
        Contact -> ActionCommand.ContactLookup(payload)
        Nav -> ActionCommand.Navigate(CommandDetector.stripNavToPrefix(payload))
        Reminder -> ActionCommand.Reminder(payload)
        Help -> ActionCommand.Help
        AddContact -> ActionCommand.AddContact(payload)
        UpdateApps -> ActionCommand.UpdateApps
        SystemSettings -> ActionCommand.SystemSettings
        LauncherSettings -> ActionCommand.LauncherSettings
        ActionBoxSettings -> ActionCommand.ActionBoxSettings
    }

    val label: String
        get() = when (this) {
            Chat -> "@  Message"
            Email -> "@  Message"
            Social -> ":  Social"
            Call -> "#  Call"
            Video -> "*  Video call"
            Translate -> "Translate"
            Notes -> "Notes"
            Sticky -> "/s  Sticky"
            Ai -> "Ask AI"
            Contact -> "Contact"
            Nav -> "/g  Go"
            Reminder -> "/r  Reminder"
            Help -> "/h  Help"
            AddContact -> "/a  Add contact"
            UpdateApps -> "/u  Update"
            SystemSettings -> "/ss  System settings"
            LauncherSettings -> "/ls  Launcher settings"
            ActionBoxSettings -> "/abs  ActionBox Settings"
        }

    /** Stays in MAD — no app picker. */
    val staysInMad: Boolean
        get() = this == Sticky || this == Help || this == UpdateApps ||
            this == SystemSettings || this == LauncherSettings || this == ActionBoxSettings
}

/** Extra phrases the user added (any language) on top of the built-in English words. */
data class UserAliasCatalog(
    val extra: Map<AliasTarget, List<String>> = emptyMap(),
) {
    fun phrases(target: AliasTarget): List<String> =
        extra[target].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun withAdded(target: AliasTarget, phrase: String): UserAliasCatalog {
        val word = phrase.trim()
        if (word.isEmpty()) return this
        val cur = phrases(target)
        if (cur.any { it.equals(word, ignoreCase = true) }) return this
        return copy(extra = extra + (target to (cur + word)))
    }

    fun withRemoved(target: AliasTarget, phrase: String): UserAliasCatalog {
        val next = phrases(target).filterNot { it.equals(phrase.trim(), ignoreCase = true) }
        return copy(
            extra = if (next.isEmpty()) extra - target else extra + (target to next),
        )
    }

    fun toJson(): String {
        val root = JSONObject()
        extra.forEach { (target, phrases) ->
            if (phrases.isNotEmpty()) {
                root.put(target.name, JSONArray(phrases))
            }
        }
        return root.toString()
    }

    companion object {
        val Empty = UserAliasCatalog()

        fun fromJson(raw: String?): UserAliasCatalog {
            if (raw.isNullOrBlank()) return Empty
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return Empty
            val map = mutableMapOf<AliasTarget, List<String>>()
            AliasTarget.entries.forEach { target ->
                val arr = obj.optJSONArray(target.name) ?: return@forEach
                val phrases = buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotEmpty()) add(s)
                    }
                }
                if (phrases.isNotEmpty()) map[target] = phrases.distinct()
            }
            return UserAliasCatalog(map)
        }
    }
}
