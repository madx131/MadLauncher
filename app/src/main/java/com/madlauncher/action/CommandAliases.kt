package com.madlauncher.action

/**
 * Word shortcuts for Action Box symbols — e.g. `msg alice` → [@] alice.
 * Built-in English words plus [UserAliasCatalog] extras (any language).
 */
object CommandAliases {
    val chat = listOf("txt", "text", "msg", "message", "mail", "email", "send")
    val call = listOf("dial", "call", "phone")
    val video = listOf("vid", "video")
    val translate = listOf("trans", "translate")
    val nav = listOf("go")
    val sticky = listOf("/s", "stk", "stick", "sticky", "sitcky")
    val help = listOf("/h")
    val contact = listOf("contact")
    val addContact = listOf("/a", "add contact")
    val updateApps = listOf("update", "updates", "update apps", "/u")
    val reminder = listOf("/r", "remind", "reminder")
    val calc = listOf("calc", "calculate", "calculator")
    val systemSettings = listOf("/ss")
    val launcherSettings = listOf("/ls")
    val actionBoxSettings = listOf("/abs")

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
        AliasTarget.Nav -> nav
        AliasTarget.Sticky -> sticky
        AliasTarget.Help -> help
        AliasTarget.Contact -> contact
        AliasTarget.AddContact -> addContact
        AliasTarget.UpdateApps -> updateApps
        AliasTarget.Reminder -> reminder
        AliasTarget.SystemSettings -> systemSettings
        AliasTarget.LauncherSettings -> launcherSettings
        AliasTarget.ActionBoxSettings -> actionBoxSettings
        AliasTarget.Email,
        AliasTarget.Social,
        AliasTarget.Notes,
        AliasTarget.Ai -> emptyList()
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
        val hit = (phraseIndex().map { it.first } + UserShortcuts.allPhrases() + calc)
            .sortedByDescending { it.length }
            .firstOrNull { it.lowercase().startsWith(q) && it.length > q.length && !it.startsWith("/") }
            ?: return null
        return "$hit "
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

    /** `? ` / `ai ` / `ask ai ` — clear field for AI prompt. */
    fun isAiCommit(query: String): Boolean {
        if (isBodyCommit(query, AliasTarget.Ai)) return true
        if (query.isEmpty() || !query.last().isWhitespace()) return false
        val trimmed = query.trimEnd()
        val lower = trimmed.lowercase()
        if (lower == "?" || lower.matches(Regex("^\\?\\s*$"))) return true
        if (lower == "ai" || lower.matches(Regex("^ai\\s*$"))) return true
        if (lower == "ask ai" || lower.matches(Regex("^ask\\s+ai\\s*$"))) return true
        if (lower == "ask ai assistant" || lower.matches(Regex("^ask\\s+ai\\s+assistant\\s*$"))) return true
        return false
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
