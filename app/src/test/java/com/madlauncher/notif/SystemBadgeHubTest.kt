package com.madlauncher.notif

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SystemBadgeHubTest {
    @Before
    fun reset() {
        SystemBadgeHub.clear()
    }

    @Test
    fun inboxDoesNotBadgeMessages() {
        SystemBadgeHub.publishNls(emptyMap())
        SystemBadgeHub.publishInbox(
            SmsInboxBadges.counts(
                unreadThreadCount = 150,
                defaultSmsPackage = "com.google.android.apps.messaging",
            ),
        )
        assertEquals(0, SystemBadgeHub.countFor("com.google.android.apps.messaging"))
        assertEquals(0, SystemBadgeHub.countFor("com.whatsapp"))
    }

    @Test
    fun shadeCountIsTheBadge() {
        SystemBadgeHub.publishNls(mapOf("com.google.android.apps.messaging" to 2))
        SystemBadgeHub.publishInbox(mapOf("com.google.android.apps.messaging" to 150))
        assertEquals(2, SystemBadgeHub.countFor("com.google.android.apps.messaging"))
    }

    @Test
    fun inboxDoesNotFillWhenShadeHasNoRow() {
        SystemBadgeHub.publishNls(mapOf("com.whatsapp" to 3))
        SystemBadgeHub.publishInbox(mapOf("com.google.android.apps.messaging" to 4))
        assertEquals(3, SystemBadgeHub.countFor("com.whatsapp"))
        assertEquals(0, SystemBadgeHub.countFor("com.google.android.apps.messaging"))
    }
}
