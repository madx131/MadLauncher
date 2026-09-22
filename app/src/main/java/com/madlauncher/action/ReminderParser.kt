package com.madlauncher.action

/** Delay vs clock time for Action Box Reminder (`/r`, remind). Clock owns the fire. */
data class ParsedReminder(
    val message: String,
    val timerSeconds: Int? = null,
    val hour: Int? = null,
    val minute: Int = 0,
)

object ReminderParser {
    private val inDuration = Regex(
        """\bin\s+(\d+)\s*(seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val trailingDuration = Regex(
        """(?:^|\s)(\d+)\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val atClock = Regex(
        """\bat\s+(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val clock = Regex(
        """\b(\d{1,2}):(\d{2})\s*(a\.?m\.?|p\.?m\.?)?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val ampm = Regex(
        """\b(\d{1,2})\s*(a\.?m\.?|p\.?m\.?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val leadIn = Regex("""^(me\s+to|me|to)\s+""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedReminder {
        val source = raw.trim()
        if (source.isEmpty()) return ParsedReminder("")

        inDuration.find(source)?.let { m ->
            val secs = toSeconds(m.groupValues[1].toInt(), m.groupValues[2])
            if (secs != null) return ParsedReminder(clean(source, m.range), timerSeconds = secs)
        }
        trailingDuration.find(source)?.let { m ->
            val secs = toSeconds(m.groupValues[1].toInt(), m.groupValues[2])
            if (secs != null) return ParsedReminder(clean(source, m.range), timerSeconds = secs)
        }
        atClock.find(source)?.let { m ->
            wall(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { (h, min) ->
                return ParsedReminder(clean(source, m.range), hour = h, minute = min)
            }
        }
        clock.find(source)?.let { m ->
            wall(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { (h, min) ->
                return ParsedReminder(clean(source, m.range), hour = h, minute = min)
            }
        }
        ampm.find(source)?.let { m ->
            wall(m.groupValues[1], "", m.groupValues[2])?.let { (h, min) ->
                return ParsedReminder(clean(source, m.range), hour = h, minute = min)
            }
        }
        return ParsedReminder(leadIn.replace(source, "").trim())
    }

    private fun clean(source: String, range: IntRange): String {
        val cut = source.removeRange(range).replace(Regex("\\s+"), " ").trim()
        return leadIn.replace(cut, "").trim()
    }

    private fun toSeconds(n: Int, unit: String): Int? {
        if (n <= 0) return null
        val u = unit.lowercase()
        val secs = when {
            u.startsWith("h") -> n * 3600
            u.startsWith("m") -> n * 60
            u.startsWith("s") -> n
            else -> return null
        }
        return secs.coerceAtMost(24 * 60 * 60)
    }

    private fun wall(hourRaw: String, minuteRaw: String, ampmRaw: String): Pair<Int, Int>? {
        val hour12 = hourRaw.toIntOrNull() ?: return null
        val minute = minuteRaw.toIntOrNull() ?: 0
        if (minute !in 0..59) return null
        val mer = ampmRaw.lowercase().replace(".", "")
        val hour = when {
            mer.startsWith("a") -> {
                if (hour12 !in 1..12) return null
                if (hour12 == 12) 0 else hour12
            }
            mer.startsWith("p") -> {
                if (hour12 !in 1..12) return null
                if (hour12 == 12) 12 else hour12 + 12
            }
            else -> {
                if (hour12 !in 0..23) return null
                hour12
            }
        }
        return hour to minute
    }
}
