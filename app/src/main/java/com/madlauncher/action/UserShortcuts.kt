package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import org.json.JSONArray
import org.json.JSONObject

enum class UserShortcutKind { OPEN_APP, COMBO }

/** Action Box prefixes a custom command can expand into. */
enum class UserShortcutCombo {
    CHAT,
    SOCIAL,
    CALL,
    VIDEO,
    ;

    val prefix: String
        get() = when (this) {
            CHAT -> "@"
            SOCIAL -> ":"
            CALL -> "#"
            VIDEO -> "*"
        }

    val appKinds: List<ActionAppKind>
        get() = when (this) {
            CHAT -> listOf(ActionAppKind.Messaging, ActionAppKind.Email)
            SOCIAL -> listOf(ActionAppKind.Social)
            CALL -> listOf(ActionAppKind.VoiceCall)
            VIDEO -> listOf(ActionAppKind.VideoCall)
        }

    val usesContact: Boolean get() = this != SOCIAL
}

data class UserShortcutContact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
) {
    val isPresent: Boolean
        get() = name.isNotBlank() || id.isNotBlank() || phone.isNotBlank() || email.isNotBlank()

    fun toEntry(): ContactEntry = ContactEntry(
        id = id.ifBlank { "shortcut:${name.ifBlank { phone.ifBlank { email } }}" },
        name = name.ifBlank { phone.ifBlank { email.ifBlank { "Contact" } } },
        phone = phone.ifBlank { null },
        email = email.ifBlank { null },
    )

    companion object {
        fun from(entry: ContactEntry): UserShortcutContact = UserShortcutContact(
            id = entry.id,
            name = entry.name,
            phone = entry.phone.orEmpty(),
            email = entry.email.orEmpty(),
        )
    }
}

/** User-defined Action Box shortcut: open an app, or expand into a command combo. */
data class UserShortcut(
    val id: String,
    val phrases: List<String>,
    val packageName: String,
    val kind: UserShortcutKind = UserShortcutKind.OPEN_APP,
    val combo: UserShortcutCombo? = null,
    val contact: UserShortcutContact? = null,
) {
    val isCombo: Boolean get() = kind == UserShortcutKind.COMBO && combo != null

    fun withPhrase(phrase: String): UserShortcut {
        val word = UserShortcuts.normalizePhrase(phrase) ?: return this
        if (phrases.any { it.equals(word, ignoreCase = true) }) return this
        return copy(phrases = phrases + word)
    }

    fun withoutPhrase(phrase: String): UserShortcut =
        copy(phrases = phrases.filterNot { it.equals(phrase.trim(), ignoreCase = true) })

    fun renamed(old: String, new: String): UserShortcut {
        val next = UserShortcuts.normalizePhrase(new) ?: return withoutPhrase(old)
        return copy(
            phrases = phrases.map { if (it.equals(old, ignoreCase = true)) next else it }
                .distinctBy { it.lowercase() },
        )
    }
}

object UserShortcuts {
    @Volatile
    var catalog: List<UserShortcut> = emptyList()
        private set

    fun replace(items: List<UserShortcut>) {
        catalog = items
    }

    fun normalizePhrase(raw: String): String? {
        val word = raw.trim().replace('／', '/')
        if (word.isEmpty()) return null
        return word
    }

    fun splitDraft(raw: String): List<String> =
        raw.split(',', '\n')
            .mapNotNull { normalizePhrase(it) }
            .distinctBy { it.lowercase() }

    fun matchDetailed(raw: String): Pair<String, ActionCommand>? {
        val trimmed = raw.trim().replace('／', '/')
        if (trimmed.isEmpty()) return null
        val hits = ArrayList<Pair<String, UserShortcut>>(8)
        catalog.forEach { shortcut ->
            shortcut.phrases.forEach { phrase ->
                hits.add(phrase to shortcut)
            }
        }
        hits.sortByDescending { it.first.length }
        for ((alias, shortcut) in hits) {
            if (trimmed.length < alias.length) continue
            if (!trimmed.regionMatches(0, alias, 0, alias.length, ignoreCase = true)) continue
            val payload = when {
                trimmed.length == alias.length -> ""
                trimmed[alias.length].isWhitespace() -> trimmed.substring(alias.length).trim()
                else -> continue
            }
            return alias to expand(shortcut, payload)
        }
        return null
    }

    fun expand(shortcut: UserShortcut, payload: String): ActionCommand {
        if (shortcut.isCombo) {
            val combo = shortcut.combo ?: UserShortcutCombo.CHAT
            val contact = shortcut.contact?.takeIf { it.isPresent }
            return ActionCommand.QuickCombo(
                combo = combo,
                packageName = shortcut.packageName,
                contactId = contact?.id.orEmpty(),
                contactName = contact?.name.orEmpty(),
                contactPhone = contact?.phone.orEmpty(),
                contactEmail = contact?.email.orEmpty(),
                payload = payload,
            )
        }
        return ActionCommand.OpenApp(shortcut.packageName, payload)
    }

    fun allPhrases(): List<String> = catalog.flatMap { it.phrases }

    fun phraseTaken(phrase: String, exceptId: String? = null): Boolean {
        val word = normalizePhrase(phrase) ?: return false
        return catalog.any { shortcut ->
            shortcut.id != exceptId && shortcut.phrases.any { it.equals(word, ignoreCase = true) }
        }
    }

    fun toJson(items: List<UserShortcut> = catalog): String {
        val arr = JSONArray()
        items.forEach { shortcut ->
            if (shortcut.phrases.isEmpty() || shortcut.packageName.isBlank()) return@forEach
            val obj = JSONObject()
                .put("id", shortcut.id)
                .put("packageName", shortcut.packageName)
                .put("phrases", JSONArray(shortcut.phrases))
                .put("kind", shortcut.kind.name)
            shortcut.combo?.let { obj.put("combo", it.name) }
            val contact = shortcut.contact?.takeIf { it.isPresent }
            if (contact != null) {
                obj.put("contactId", contact.id)
                    .put("contactName", contact.name)
                    .put("contactPhone", contact.phone)
                    .put("contactEmail", contact.email)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun fromJson(raw: String?): List<UserShortcut> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<UserShortcut>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val pkg = obj.optString("packageName").trim()
            if (pkg.isEmpty()) continue
            val phrasesArr = obj.optJSONArray("phrases") ?: continue
            val phrases = buildList {
                for (p in 0 until phrasesArr.length()) {
                    normalizePhrase(phrasesArr.optString(p))?.let { add(it) }
                }
            }.distinctBy { it.lowercase() }
            if (phrases.isEmpty()) continue
            val id = obj.optString("id").ifBlank { System.currentTimeMillis().toString() + i }
            val kind = runCatching { UserShortcutKind.valueOf(obj.optString("kind")) }
                .getOrDefault(UserShortcutKind.OPEN_APP)
            val combo = obj.optString("combo").takeIf { it.isNotBlank() }?.let {
                runCatching { UserShortcutCombo.valueOf(it) }.getOrNull()
            }
            val resolvedKind = if (kind == UserShortcutKind.COMBO && combo == null) {
                UserShortcutKind.OPEN_APP
            } else {
                kind
            }
            val contact = UserShortcutContact(
                id = obj.optString("contactId"),
                name = obj.optString("contactName"),
                phone = obj.optString("contactPhone"),
                email = obj.optString("contactEmail"),
            ).takeIf { it.isPresent }
            out.add(
                UserShortcut(
                    id = id,
                    phrases = phrases,
                    packageName = pkg,
                    kind = resolvedKind,
                    combo = if (resolvedKind == UserShortcutKind.COMBO) combo else null,
                    contact = if (resolvedKind == UserShortcutKind.COMBO) contact else null,
                ),
            )
        }
        return out
    }
}
