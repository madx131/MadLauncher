package com.madlauncher.action

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCommandAppsTest {
    private fun app(pkg: String, name: String = pkg) = InstalledActionApp(
        name = name,
        packageName = pkg,
        color = Color.Gray,
        icon = null,
    )

    @Test
    fun arrange_putsSystemDefaultFirst() {
        val apps = listOf(app("com.whatsapp"), app("com.google.android.apps.messaging"))
        val ranked = DefaultCommandApps.arrange(
            apps = apps,
            defaultPackage = "com.google.android.apps.messaging",
            preferredPackage = "com.whatsapp",
            hidden = emptySet(),
        )
        assertEquals(
            listOf("com.google.android.apps.messaging", "com.whatsapp"),
            ranked.map { it.packageName },
        )
    }

    @Test
    fun arrange_hidesRemovedDefault() {
        val apps = listOf(app("sms", "Messages"), app("wa", "WhatsApp"))
        val ranked = DefaultCommandApps.arrange(
            apps = apps,
            defaultPackage = "sms",
            preferredPackage = null,
            hidden = setOf("sms"),
        )
        assertEquals(listOf("wa"), ranked.map { it.packageName })
    }

    @Test
    fun arrange_followsUserOrderAfterPinnedDefault() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val ranked = DefaultCommandApps.arrange(
            apps = apps,
            defaultPackage = "a",
            preferredPackage = null,
            hidden = emptySet(),
            order = listOf("c", "b", "a"),
        )
        assertEquals(listOf("a", "c", "b"), ranked.map { it.packageName })
    }

    @Test
    fun arrange_dropsHiddenNonDefault() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val ranked = DefaultCommandApps.arrange(
            apps = apps,
            defaultPackage = "a",
            preferredPackage = null,
            hidden = setOf("b"),
        )
        assertEquals(listOf("a", "c"), ranked.map { it.packageName })
        assertTrue(ranked.none { it.packageName == "b" })
    }

    @Test
    fun pickAddContactTarget_skipsEyeconAndPrefersSystemContacts() {
        val eyecon = app("com.eyecon.global", "Eyecon")
        val contacts = app("com.google.android.contacts", "Contacts")
        val picked = DefaultCommandApps.pickAddContactTarget(
            apps = listOf(eyecon, contacts),
            systemDefault = "com.eyecon.global",
        )
        assertEquals("com.google.android.contacts", picked?.packageName)
    }

    @Test
    fun pickAddContactTarget_nullWhenOnlyEyecon() {
        val picked = DefaultCommandApps.pickAddContactTarget(
            apps = listOf(app("com.eyecon.global", "Eyecon")),
            systemDefault = "com.eyecon.global",
        )
        assertNull(picked)
    }

    @Test
    fun arrange_keepsEyeconForDialerLists() {
        val ranked = DefaultCommandApps.arrange(
            apps = listOf(app("com.eyecon.global", "Eyecon"), app("com.google.android.dialer")),
            defaultPackage = "com.eyecon.global",
            preferredPackage = null,
            hidden = emptySet(),
        )
        assertEquals("com.eyecon.global", ranked.first().packageName)
        assertTrue(ranked.any { it.packageName == "com.google.android.dialer" })
    }
}
