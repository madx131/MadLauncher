package com.madlauncher.action

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAppRegistryTest {
    @Test
    fun catalog_voiceIncludesOnlySupportedApps() {
        val names = CallAppRegistry.catalogNames(CallAppMode.Voice)
        assertTrue(names.any { it.equals("Phone", ignoreCase = true) })
        assertTrue(names.any { it.equals("WhatsApp", ignoreCase = true) })
        assertFalse(names.any { it.equals("Messenger", ignoreCase = true) })
    }

    @Test
    fun unsupportedCatalog_listsOttApps() {
        val names = CallAppRegistry.unsupportedCatalogNames(CallAppMode.Voice)
        assertTrue(names.any { it.equals("Messenger", ignoreCase = true) })
        assertTrue(names.any { it.equals("Telegram", ignoreCase = true) })
    }

    @Test
    fun isSupportedCallPackage_whatsappAndDialerOnly() {
        assertTrue(CallAppRegistry.isSupportedCallPackage("com.whatsapp"))
        assertTrue(CallAppRegistry.isSupportedCallPackage("com.google.android.dialer"))
        assertFalse(CallAppRegistry.isSupportedCallPackage("com.facebook.orca"))
        assertFalse(CallAppRegistry.isSupportedCallPackage("org.telegram.messenger"))
    }

    @Test
    fun historyPackages_includeMessagingApps() {
        val pkgs = CallAppRegistry.historyPackages()
        assertTrue("com.whatsapp" in pkgs)
        assertTrue("com.facebook.orca" in pkgs)
    }

    @Test
    fun isHistoryPackage_messengerYes_browserNo() {
        assertTrue(CallAppRegistry.isHistoryPackage("com.facebook.orca"))
        assertFalse(CallAppRegistry.isHistoryPackage("com.android.chrome"))
    }
}
