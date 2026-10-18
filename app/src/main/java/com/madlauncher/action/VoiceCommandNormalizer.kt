package com.madlauncher.action

/**
 * Spoken Action Box phrases become the compact command that gets typed and run.
 * "one" → `1`. "message mom" → `msg+mom`.
 */
object VoiceCommandNormalizer {
    private val numberWords = mapOf(
        "zero" to "0", "oh" to "0",
        "one" to "1", "won" to "1",
        "two" to "2", "to" to "2", "too" to "2",
        "three" to "3",
        "four" to "4", "for" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8", "ate" to "8",
        "nine" to "9",
        "ten" to "10",
        "eleven" to "11",
        "twelve" to "12",
    )

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
        val words = lower.split(' ')
        if (words.all { it in numberWords }) {
            return words.joinToString("") { numberWords.getValue(it) }
        }
        val aliasHit = CommandAliases.matchDetailed(cleaned) ?: return cleaned
        val target = AliasTarget.entries.firstOrNull { t ->
            CommandAliases.allPhrases(t).any { it.equals(aliasHit.first, ignoreCase = true) }
        } ?: return cleaned
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
        val short = shortAlias(target)
        return if (payload.isEmpty()) short else "$short+$payload"
    }
}
