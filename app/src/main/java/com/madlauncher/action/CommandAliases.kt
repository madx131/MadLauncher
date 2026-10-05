package com.madlauncher.action

/**
 * `!` is always a website search in the browser (DDG extras).
 * In-app search is Space on a common app (`yt `, `gm `), not a bang.
 */
object AppBangs {
    fun owns(@Suppress("UNUSED_PARAMETER") trigger: String): Boolean = false
}

/**
 * Word shortcuts for Action Box symbols — e.g. `msg alice` → [@] alice.
 * Built-in English words plus [UserAliasCatalog] extras (any language).
 */
object CommandAliases {
    val chat = listOf("txt", "text", "msg", "message", "mail", "email", "send")
    val social = listOf("post", "social")
    val call = listOf("dial", "call", "phone")
    val video = listOf("vid", "video")
    val translate = listOf("trans", "translate")
    val notes = listOf("note", "notes")
    val nav = listOf("go")
    val sticky = listOf("/s", "stk", "stick", "sticky", "sitcky")
    val help = listOf("/h", "help")
    val contact = listOf("contact")
    val addContact = listOf("/a", "add contact")
    val updateApps = listOf("update", "updates", "update apps", "/u")
    val reminder = listOf("/r", "remind", "reminder")
    val schedule = listOf("/c", "schedule", "event")
    val ai = listOf("ai", "ask ai")
    val calc = listOf("calc", "calculate", "calculator")
    val systemSettings = listOf("/ss", "ss")
    val launcherSettings = listOf("/ls", "ls")
    val actionBoxSettings = listOf("/abs", "abs")

    @Volatile
    var extras: UserAliasCatalog = UserAliasCatalog.Empty
        private set

    @Volatile
    var disabledBuiltIns: Set<String> = emptySet()
        private set

    fun replaceExtras(catalog: UserAliasCatalog) {
        extras = catalog
    }

    fun replaceDisabledBuiltIns(disabled: Set<String>) {
        disabledBuiltIns = disabled.map { it.lowercase() }.toSet()
    }

    fun stockPhrases(target: AliasTarget): List<String> = when (target) {
        AliasTarget.Chat -> chat
        AliasTarget.Call -> call
        AliasTarget.Video -> video
        AliasTarget.Translate -> translate
        AliasTarget.Notes -> notes
        AliasTarget.Nav -> nav
        AliasTarget.Sticky -> sticky
        AliasTarget.Help -> help
        AliasTarget.Contact -> contact
        AliasTarget.AddContact -> addContact
        AliasTarget.UpdateApps -> updateApps
        AliasTarget.Reminder -> reminder
        AliasTarget.Schedule -> schedule
        AliasTarget.Social -> social
        AliasTarget.Ai -> ai
        AliasTarget.SystemSettings -> systemSettings
        AliasTarget.LauncherSettings -> launcherSettings
        AliasTarget.ActionBoxSettings -> actionBoxSettings
        AliasTarget.Email -> emptyList()
    }

    fun builtIn(target: AliasTarget): List<String> =
        stockPhrases(target).filter { it.lowercase() !in disabledBuiltIns }

    fun allPhrases(target: AliasTarget): List<String> {
        val extra = extras.phrases(target)
        return (builtIn(target) + extra).distinctBy { it.lowercase() }
    }

    private fun phraseIndex(): List<Pair<String, AliasTarget>> {
        val out = ArrayList<Pair<String, AliasTarget>>(64)
        AliasTarget.entries.forEach { target ->
            allPhrases(target).forEach { phrase ->
                out.add(phrase to target)
            }
        }
        return out.sortedByDescending { it.first.length }
    }

    fun isActive(target: AliasTarget, phrase: String): Boolean =
        allPhrases(target).any { it.equals(phrase, ignoreCase = true) }

    fun matchDetailed(raw: String): Pair<String, ActionCommand>? {
        val trimmed = raw.trim().replace('／', '/')
        if (trimmed.isEmpty()) return null
        for ((alias, target) in phraseIndex()) {
            if (trimmed.length < alias.length) continue
            if (!trimmed.regionMatches(0, alias, 0, alias.length, ignoreCase = true)) continue
            if (trimmed.length == alias.length) {
                return alias to target.toCommand("")
            }
            if (trimmed[alias.length].isWhitespace()) {
                return alias to target.toCommand(trimmed.substring(alias.length).trim())
            }
        }
        return null
    }

    fun match(raw: String): ActionCommand? = matchDetailed(raw)?.second

    /** First-word alias completion, e.g. `mes` → `message `. */
    fun autocomplete(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        if (trimmed.startsWith("@") || trimmed.startsWith("#") || trimmed.startsWith("*") ||
            trimmed.startsWith("/") || trimmed.startsWith(":") || trimmed.startsWith("=") ||
            trimmed.startsWith("&") || trimmed.startsWith("?") || trimmed.startsWith("!")
        ) {
            return null
        }
        val q = trimmed.lowercase()
        fun best(from: List<String>): String? = from
            .sortedByDescending { it.length }
            .firstOrNull { it.lowercase().startsWith(q) && it.length > q.length && !it.startsWith("/") }
        best(phraseIndex().map { it.first } + UserShortcuts.allPhrases() + calc)?.let { return "$it " }
        best(CommonAppShortcuts.allPhrases())?.let { return "$it " }
        return null
    }

    fun isTranslateQuery(query: String): Boolean = translatePrefix(query) != null

    /**
     * True when the user just committed a translate command with a trailing space
     * and no body yet — e.g. `/t `, `trans `, `translate `.
     */
    fun isTranslateCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Translate) ||
        slashCommit(query, "/t")

    /** `/g ` or `go ` — clear field for a destination. */
    fun isNavCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Nav) ||
        slashCommit(query, "/g")

    /** `calc ` / `calculate ` — switch Action Box to the calculator keypad. */
    fun isCalcCommit(query: String): Boolean {
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val trimmed = query.trimEnd().lowercase()
        return calc.any { it.equals(trimmed, ignoreCase = true) }
    }

    /** `msg ` — messaging app picker. `@` stays in the field as a filter. */
    fun isChatCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Chat)

    /** `post ` — social composer picker. `:` stays in the field as a filter. */
    fun isSocialCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Social)

    /** `call ` — call someone. `#` stays in the field as a filter. */
    fun isCallCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Call)

    /** `video ` — video call. `*` stays in the field as a filter. */
    fun isVideoCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Video)

    /** `sticky ` / `/s ` — floating sticky note. */
    fun isStickyCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Sticky) ||
        slashCommit(query, "/s")

    /** `remind ` / `/r ` — reminder compose. */
    fun isReminderCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Reminder) ||
        slashCommit(query, "/r")

    /** `schedule ` / `/c ` — calendar event. */
    fun isScheduleCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Schedule) ||
        slashCommit(query, "/c")

    /** `/n ` — clear field for note body. */
    fun isNotesCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Notes) ||
        slashCommit(query, "/n")

    /** `/a ` or `add contact ` — clear field for name/number. */
    fun isAddContactCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.AddContact) ||
        slashCommit(query, "/a")

    /** `/d ` or a custom phrase + space — clear field for that shortcut's payload. */
    fun isUserShortcutCommit(query: String): Boolean {
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val trimmed = query.trimEnd()
        val hit = UserShortcuts.matchDetailed(trimmed) ?: return false
        return hit.second is ActionCommand.OpenApp &&
            (hit.second as ActionCommand.OpenApp).payload.isEmpty()
    }

    /** `ai ` / `ask ai ` — clear field for AI prompt. `?` stays in the field as a filter. */
    fun isAiCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Ai)

    /**
     * `@ ` `# ` `: ` `* ` `? ` — Space addon on the first app in that command's
     * ActionBox Settings list. `@wa` still filters; only the bare symbol + space commits.
     */
    fun isSymbolShortcutCommit(query: String): Boolean {
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val trimmed = query.trimEnd()
        return trimmed == "@" || trimmed == "#" || trimmed == ":" || trimmed == "：" ||
            trimmed == "*" || trimmed == "?"
    }

    /** `& ` — clear field for contact search. */
    fun isContactLookupCommit(query: String): Boolean = isBodyCommit(query, AliasTarget.Contact) ||
        run {
            if (query.isEmpty() || !query.last().isWhitespace()) return@run false
            val lower = query.trimEnd().lowercase()
            lower == "&" || lower.matches(Regex("^&\\s*$"))
        }

    fun translateTail(query: String): String =
        translatePrefix(query)?.second ?: query.trim()

    /** Prefix string (with trailing space) and tail after the translate command token. */
    fun translatePrefix(query: String): Pair<String, String>? {
        val trimmed = query.trim().replace('／', '/')
        val lower = trimmed.lowercase()
        if (lower == "/t") return "/t " to ""
        if (lower.startsWith("/t ")) return "/t " to trimmed.substring(2).trimStart()
        for (alias in allPhrases(AliasTarget.Translate).sortedByDescending { it.length }) {
            if (lower == alias.lowercase()) return "$alias " to ""
            if (lower.startsWith(alias.lowercase() + " ")) {
                return trimmed.substring(0, alias.length) + " " to trimmed.substring(alias.length).trimStart()
            }
        }
        return null
    }

    private fun isBodyCommit(query: String, target: AliasTarget): Boolean {
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val trimmed = query.trimEnd()
        return allPhrases(target).any { alias ->
            trimmed.equals(alias, ignoreCase = true)
        }
    }

    private fun slashCommit(query: String, token: String): Boolean {
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val lower = query.trim().replace('／', '/').lowercase()
        return lower == token || lower.matches(Regex("^${Regex.escape(token)}\\s*$"))
    }
}
