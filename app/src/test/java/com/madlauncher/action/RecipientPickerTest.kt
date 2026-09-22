package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecipientPickerTest {
    @Test
    fun mergeRecentAndPhone_historyFirst_dedupesByPhone() {
        val recent = listOf(
            ContactEntry(id = "app|1", name = "Alice", phone = "+15550100", fromAppHistory = true),
        )
        val phone = listOf(
            ContactEntry(id = "42", name = "Alice Smith", phone = "+1 555-0100"),
            ContactEntry(id = "43", name = "Bob", phone = "+15550101"),
        )
        val merged = RecipientPicker.mergeRecentAndPhone(recent, phone)
        assertEquals(2, merged.size)
        assertEquals("Alice", merged[0].name)
        assertEquals("Bob", merged[1].name)
        assertFalse(merged[1].fromAppHistory)
    }

    @Test
    fun partition_splitsByFromAppHistory() {
        val merged = listOf(
            ContactEntry(id = "a", name = "A", fromAppHistory = true),
            ContactEntry(id = "b", name = "B", fromAppHistory = false),
        )
        val (recent, phone) = RecipientPicker.partition(merged)
        assertEquals(1, recent.size)
        assertEquals(1, phone.size)
    }
}
