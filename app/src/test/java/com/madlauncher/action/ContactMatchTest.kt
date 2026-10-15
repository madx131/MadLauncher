package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactMatchTest {
    @Test
    fun bestNameIsFirst() {
        val contacts = listOf(
            ContactEntry("g", "Barak Ben David and Tomer Ben David", fromAppHistory = true),
            ContactEntry("p", "Barak Ben David"),
            ContactEntry("o", "Orit, Elizabeth, Alek and Tomer", fromAppHistory = true),
            ContactEntry("ob", "Orit Brener"),
            ContactEntry("sb", "Svety Brener"),
        )
        assertEquals("Barak Ben David", ContactMatch.order("barak", contacts).first().name)
        assertEquals("Barak Ben David", ContactMatch.order("barak ben david", contacts).first().name)
        assertEquals("Orit Brener", ContactMatch.order("orit", contacts).first().name)
        assertEquals("Svety Brener", ContactMatch.order("svety", contacts).first().name)
    }

    @Test
    fun apostropheAndOneLetterStillRankThePersonFirst() {
        val contacts = listOf(
            ContactEntry("g", "Pe'er Ben David and Tomer Ben David", fromAppHistory = true),
            ContactEntry("b", "Barak Ben David"),
            ContactEntry("p", "Pe'er Ben David"),
        )
        assertEquals("Pe'er Ben David", ContactMatch.order("pe'er", contacts).first().name)
        assertEquals("Pe'er Ben David", ContactMatch.order("peer", contacts).first().name)
        assertEquals("Pe'er Ben David", ContactMatch.order("pe'er ban david", contacts).first().name)
        assertEquals("Pe'er Ben David", ContactMatch.order("pe'er ben david", contacts).first().name)
    }
}
