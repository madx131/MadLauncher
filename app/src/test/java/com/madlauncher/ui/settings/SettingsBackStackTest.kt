package com.madlauncher.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsBackStackTest {
    @Test
    fun extrasNesting() {
        assertEquals(
            SettingsRoute.EXPERIMENTAL,
            SettingsBackStack.parent(SettingsRoute.NAV_SHORTCUTS, fromActionBox = false, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.EXPERIMENTAL,
            SettingsBackStack.parent(SettingsRoute.SOCIAL_BROWSER, fromActionBox = false, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.SOCIAL_BROWSER,
            SettingsBackStack.parent(SettingsRoute.SOCIAL_BROWSER_ABSENT, fromActionBox = false, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.EXTRAS,
            SettingsBackStack.parent(SettingsRoute.EXPERIMENTAL, fromActionBox = false, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.EXTRAS,
            SettingsBackStack.parent(SettingsRoute.NOT_WORKING, fromActionBox = false, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.HUB,
            SettingsBackStack.parent(SettingsRoute.EXTRAS, fromActionBox = false, independentActionBox = false),
        )
        assertNull(
            SettingsBackStack.parent(SettingsRoute.HUB, fromActionBox = false, independentActionBox = false),
        )
    }

    @Test
    fun experimentalBackGoesToExtrasNotHub() {
        assertEquals(
            SettingsRoute.EXTRAS,
            SettingsBackStack.parent(SettingsRoute.EXPERIMENTAL, fromActionBox = true, independentActionBox = true),
        )
        assertEquals(
            SettingsRoute.EXTRAS,
            SettingsBackStack.parent(SettingsRoute.NOT_WORKING, fromActionBox = true, independentActionBox = false),
        )
    }

    @Test
    fun actionBoxCommands() {
        assertEquals(
            SettingsRoute.EDIT_DOCKS,
            SettingsBackStack.parent(SettingsRoute.ACTION_BOX_COMMANDS, fromActionBox = false, independentActionBox = false),
        )
        assertNull(
            SettingsBackStack.parent(SettingsRoute.ACTION_BOX_COMMANDS, fromActionBox = true, independentActionBox = false),
        )
        assertEquals(
            SettingsRoute.HUB,
            SettingsBackStack.parent(SettingsRoute.ACTION_BOX_COMMANDS, fromActionBox = true, independentActionBox = true),
        )
    }
}
