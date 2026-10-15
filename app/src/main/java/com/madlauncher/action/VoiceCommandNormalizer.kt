package com.madlauncher.action

/**
 * Turns spoken Action Box phrases into compact typed commands.
 *
 * Examples:
 * - "one" → `1`
 * - "message mom" → `msg+mom`
 * - "call dad" → `call+dad`
 */
object VoiceCommandNormalizer {
    private val numberWords = mapOf(
        "zero" to "0", "oh" to "0",
        "one" to "1", "won" to "1",
        "two" to "2", "to" to "2", "too" to "2",
        "three" to "3", "tree" to "3",
        "four" to "4", "for" to "4", "fore" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8", "ate" to "8",
        "nine" to "9",
        "ten" to "10",
        "eleven" to "11",
        "twelve" to "12",
    )

    /** Preferred short token written into the Action Box for each [AliasTarget]. */
    private fun shortAlias(target: AliasTarget): String = when (target) {
        AliasTarget.Chat, AliasTarget.Email -> "msg"
        AliasTarget.Social -> "post"
        AliasTarget.Call -> "call"
        AliasTarget.Video -> "video"
        AliasTarget.Translate -> "trans"
        AliasTarget.Notes -> "note"
        AliasTarget.Sticky -> "sticky"
        AliasTarget.Ai -> "ai"
        AliasTarget.Contact -> "contact"
        AliasTarget.Nav -> "go"
        AliasTarget.Reminder -> "remind"
        AliasTarget.Schedule -> "schedule"
        AliasTarget.Help -> "help"
        AliasTarget.AddContact -> "add contact"
        AliasTarget.UpdateApps -> "update"
        AliasTarget.SystemSettings -> "ss"
        AliasTarget.LauncherSettings -> "ls"
        AliasTarget.ActionBoxSettings -> "abs"
    }

    fun normalize(spoken: String): String {
        val cleaned = spoken.trim()
            .replace(Regex("[.!?…,]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return ""

        val lower = cleaned.lowercase()
        numberWords[lower]?.let { return it }

        // Whole phrase is only number words → digits (e.g. "one two" → "12").
        val words = lower.split(' ')
        if (words.isNotEmpty() && words.all { it in numberWords }) {
            return words.joinToString("") { numberWords.getValue(it) }
        }

        // Longest alias match → short+payload (message mom → msg+mom).
        val aliasHit = CommandAliases.matchDetailed(cleaned)
        if (aliasHit != null) {
            val target = AliasTarget.entries.firstOrNull { t ->
                CommandAliases.allPhrases(t).any { it.equals(aliasHit.first, ignoreCase = true) }
            }
            if (target != null) {
                val short = shortAlias(target)
                val payload = when (val cmd = aliasHit.second) {
                    is ActionCommand.ChatPick -> cmd.filter
                    is ActionCommand.PostPick -> cmd.filter
                    is ActionCommand.Call -> cmd.query
                    is ActionCommand.VideoCall -> cmd.query
                    is ActionCommand.AiAsk -> cmd.prompt
                    is ActionCommand.NotePick -> cmd.text
                    is ActionCommand.Note -> cmd.text
                    is ActionCommand.Translate -> cmd.tail
                    is ActionCommand.Navigate -> cmd.destination
                    is ActionCommand.Reminder -> cmd.text
                    is ActionCommand.Schedule -> cmd.activity
                    is ActionCommand.AddContact -> cmd.details
                    is ActionCommand.ContactLookup -> cmd.filter
                    else -> ""
                }.trim()
                return if (payload.isEmpty()) short else "$short+$payload"
            }
        }

        // Lone number-word tokens inside otherwise free text.
        if (words.size == 1) {
            numberWords[words[0]]?.let { return it }
        }

        return cleaned
    }
}
