package com.madlauncher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactEntry(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    /** From MAD notification / conversation history, not the device address book. */
    val fromAppHistory: Boolean = false,
)

class ContactsRepository(private val context: Context) {
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun search(query: String, emails: Boolean = false): List<ContactEntry> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val q = query.trim()
        if (emails) queryEmails(q) else queryPhones(q)
    }

    private fun queryPhones(query: String): List<ContactEntry> {
        val needles = NameKey.needles(query)
        if (needles.isEmpty()) return queryPhoneRows(null, null)
        val out = linkedMapOf<String, ContactEntry>()
        val column = foldedColumn(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        for (needle in needles) {
            for (entry in queryPhoneRows("$column LIKE ?", arrayOf("%$needle%"))) {
                out.putIfAbsent(entry.id, entry)
            }
        }
        return out.values.toList()
    }

    private fun queryPhoneRows(selection: String?, args: Array<String>?): List<ContactEntry> {
        val cr = context.contentResolver
        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            selection,
            args,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        ) ?: return emptyList()
        val out = LinkedHashMap<String, ContactEntry>()
        cursor.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = it.getString(idIdx) ?: continue
                if (out.containsKey(id)) continue
                out[id] = ContactEntry(
                    id = id,
                    name = it.getString(nameIdx) ?: "Unknown",
                    phone = it.getString(numIdx),
                )
            }
        }
        return out.values.toList().take(80)
    }

    private fun queryEmails(query: String): List<ContactEntry> {
        val needles = NameKey.needles(query)
        if (needles.isEmpty()) return queryEmailRows(null, null)
        val out = linkedMapOf<String, ContactEntry>()
        val column = foldedColumn(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
        for (needle in needles) {
            for (entry in queryEmailRows("$column LIKE ?", arrayOf("%$needle%"))) {
                out.putIfAbsent(entry.id, entry)
            }
        }
        return out.values.toList()
    }

    private fun queryEmailRows(selection: String?, args: Array<String>?): List<ContactEntry> {
        val cr = context.contentResolver
        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
            ),
            selection,
            args,
            "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME} ASC",
        ) ?: return emptyList()
        val out = LinkedHashMap<String, ContactEntry>()
        cursor.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val mailIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                val id = it.getString(idIdx) ?: continue
                if (out.containsKey(id)) continue
                out[id] = ContactEntry(
                    id = id,
                    name = it.getString(nameIdx) ?: "Unknown",
                    email = it.getString(mailIdx),
                )
            }
        }
        return out.values.toList().take(80)
    }

    /** Apostrophes removed so Pe'er matches a search for peer. */
    private fun foldedColumn(column: String): String {
        var expr = "LOWER($column)"
        for (mark in listOf("'", "’", "ʼ", "`", "׳", "´")) {
            expr = "REPLACE($expr, '${mark.replace("'", "''")}', '')"
        }
        return expr
    }
}
