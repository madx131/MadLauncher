package com.madlauncher.action

import com.madlauncher.data.ContactEntry

sealed class ActionCommand {
    data object None : ActionCommand()
    data class ChatPick(val filter: String) : ActionCommand()
    data class PostPick(val filter: String) : ActionCommand()
    /** Contact-first (&): pick person, then app, then message or call. */
    data class ContactLookup(val filter: String) : ActionCommand()
    /** Ask an installed AI assistant (?); [prompt] is sent to start the conversation. */
    data class AiAsk(val prompt: String) : ActionCommand()
    data class Reminder(val text: String) : ActionCommand()
    data class Call(val query: String) : ActionCommand()
    data class VideoCall(val query: String) : ActionCommand()
    /** Sticky note (/s). */
    data class Note(val text: String, val floating: Boolean) : ActionCommand()
    /** Pick a notes app (/n) and create the note there. */
    data class NotePick(val text: String) : ActionCommand()
    /** Open translator (/t) with optional from/to languages and text. */
    data class Translate(val tail: String) : ActionCommand()
    data class Schedule(val activity: String) : ActionCommand()
    data object LauncherSettings : ActionCommand()
    data object ActionBoxSettings : ActionCommand()
    data object SystemSettings : ActionCommand()
    data object Help : ActionCommand()
    data object Emergency : ActionCommand()
    /** Opens GetApps / Play Store updates (not a silent install). */
    data object UpdateApps : ActionCommand()
    /** Hidden Google-style Easter egg — 360° spin. */
    data object BarrelRoll : ActionCommand()
    /** `+` / `+ shop` — create a Your-commands shortcut from the Action Box pill. */
    data class AddUserCommand(val phrase: String) : ActionCommand()
    data class Navigate(val destination: String) : ActionCommand()
    /** User-defined phrase (e.g. /d, do, stick picture) opens [packageName]. */
    data class OpenApp(val packageName: String, val payload: String) : ActionCommand()
    /** Custom shortcut that expands into @ / ! / # / * with an app and optional contact. */
    data class QuickCombo(
        val combo: UserShortcutCombo,
        val packageName: String,
        val contactId: String = "",
        val contactName: String = "",
        val contactPhone: String = "",
        val contactEmail: String = "",
        val payload: String = "",
    ) : ActionCommand() {
        val hasContact: Boolean
            get() = contactName.isNotBlank() || contactId.isNotBlank() ||
                contactPhone.isNotBlank() || contactEmail.isNotBlank()

        fun toContact(): ContactEntry? {
            if (!hasContact) return null
            return ContactEntry(
                id = contactId.ifBlank { "shortcut:${contactName.ifBlank { contactPhone }}" },
                name = contactName.ifBlank { contactPhone.ifBlank { contactEmail.ifBlank { "Contact" } } },
                phone = contactPhone.ifBlank { null },
                email = contactEmail.ifBlank { null },
            )
        }
    }
    /** Pick a contacts app (/a) and open add-contact with optional name/number. */
    data class AddContact(val details: String) : ActionCommand()
    data class Calc(val result: CalcResult) : ActionCommand()
}

object CommandDetector {
    private val navRegex = Regex("^(nav|navigate|go to)\\b\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val navToPrefix = Regex("^to\\s+", RegexOption.IGNORE_CASE)

    /** Drop a leading "to " so `/g to paris` and `go to paris` both destine paris. */
    fun stripNavToPrefix(dest: String): String = dest.replace(navToPrefix, "").trim()

    private val askAiRegex = Regex("^(ask\\s+ai(?:\\s+assistant)?|ai)\\b\\s*(.*)$", RegexOption.IGNORE_CASE)

    fun isPhoneLike(s: String): Boolean {
        val digits = s.filter { it.isDigit() }
        return digits.length >= 3 && s.trim().matches(Regex("^[\\d\\s+()-]+$"))
    }

    fun detect(raw: String): ActionCommand {
        if (raw.isEmpty()) return ActionCommand.None
        val trimmedLower = raw.trim().lowercase()
        if (trimmedLower == "911" || trimmedLower.startsWith("911 ")) {
            return ActionCommand.Emergency
        }
        if (isBarrelRoll(raw)) return ActionCommand.BarrelRoll

        val alias = CommandAliases.matchDetailed(raw)
        val custom = UserShortcuts.matchDetailed(raw)
        when {
            alias != null && custom != null ->
                return if (custom.first.length > alias.first.length) custom.second else alias.second
            alias != null -> return alias.second
            custom != null -> return custom.second
        }

        val plus = raw.trim().replace('＋', '+')
        if (plus == "+") return ActionCommand.AddUserCommand("")
        if (plus.startsWith("+ ") || plus.startsWith("+\t")) {
            return ActionCommand.AddUserCommand(plus.substring(1).trim())
        }

        if (raw.startsWith("@")) {
            val body = raw.dropWhile { it == '@' }
            return ActionCommand.ChatPick(body)
        }
        if (raw.startsWith("!")) return ActionCommand.PostPick(raw.substring(1))
        if (raw.startsWith("?")) return ActionCommand.AiAsk(raw.substring(1).trim())
        if (raw.startsWith("&")) return ActionCommand.ContactLookup(raw.substring(1).trimStart())
        if (raw.startsWith("*")) return ActionCommand.VideoCall(raw.substring(1))
        if (raw.startsWith("#")) return ActionCommand.Call(raw.substring(1))

        val askAi = askAiRegex.matchEntire(raw.trim())
        if (askAi != null) {
            return ActionCommand.AiAsk(askAi.groupValues[2].trim())
        }

        val slash = raw.trim().replace('／', '/')
        val lower = slash.lowercase()
        when {
            (lower == "/ss" || lower.startsWith("/ss ")) &&
                CommandAliases.isActive(AliasTarget.SystemSettings, "/ss") ->
                return ActionCommand.SystemSettings
            (lower == "/ls" || lower.startsWith("/ls ")) &&
                CommandAliases.isActive(AliasTarget.LauncherSettings, "/ls") ->
                return ActionCommand.LauncherSettings
            (lower == "/abs" || lower.startsWith("/abs ")) &&
                CommandAliases.isActive(AliasTarget.ActionBoxSettings, "/abs") ->
                return ActionCommand.ActionBoxSettings
            (lower == "/s" || lower.startsWith("/s ")) && CommandAliases.isActive(AliasTarget.Sticky, "/s") -> {
                val text = slash.substring(2).trim()
                if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {
                    return ActionCommand.NotePick(text)
                }
                return ActionCommand.Note(text, floating = true)
            }
            lower == "/n" || lower.startsWith("/n ") -> {
                val text = raw.trim().substring(2).trim()
                return ActionCommand.NotePick(text)
            }
            lower == "/t" || lower.startsWith("/t ") -> {
                val text = raw.trim().substring(2).trim()
                return ActionCommand.Translate(text)
            }
            lower == "/g" || lower.startsWith("/g ") -> {
                val dest = slash.substring(2).trim()
                return ActionCommand.Navigate(stripNavToPrefix(dest))
            }
            (lower == "/a" || lower.startsWith("/a ")) && CommandAliases.isActive(AliasTarget.AddContact, "/a") -> {
                val details = slash.substring(2).trim()
                return ActionCommand.AddContact(details)
            }
            (lower == "/r" || lower.startsWith("/r ")) && CommandAliases.isActive(AliasTarget.Reminder, "/r") -> {
                return ActionCommand.Reminder(slash.substring(2).trim())
            }
            lower.startsWith("/c") -> return ActionCommand.Schedule(raw.substring(2).trim())
            (lower == "/h" || lower.startsWith("/h ")) && CommandAliases.isActive(AliasTarget.Help, "/h") ->
                return ActionCommand.Help
        }

        val nav = navRegex.matchEntire(raw.trim())
        if (nav != null) {
            return ActionCommand.Navigate(stripNavToPrefix(nav.groupValues[2]))
        }

        CalcParser.tryParse(raw)?.let { result ->
            if (InstantModules.isEnabled(InstantModules.moduleIdFor(result))) {
                return ActionCommand.Calc(result)
            }
        }

        return ActionCommand.None
    }

    /** Google's "do a barrel roll" — exact-ish phrase, before aliases so `do` shortcuts cannot steal it. */
    fun isBarrelRoll(raw: String): Boolean {
        val n = raw.trim().lowercase()
            .replace(Regex("[.!?…]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return n == "do a barrel roll" || n == "barrel roll" || n == "do a barrelroll"
    }
}
