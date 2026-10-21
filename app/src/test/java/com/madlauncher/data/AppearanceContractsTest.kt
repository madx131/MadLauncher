package com.madlauncher.data

import com.madlauncher.ui.theme.MadTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceContractsTest {
    @Test
    fun cornerDpMatchesShapeContract() {
        assertEquals(999f, MadTheme.cornerDpFor(IconShapeId.CIRCLE), 0.01f)
        assertEquals(0f, MadTheme.cornerDpFor(IconShapeId.SQUARE), 0.01f)
        assertEquals(0f, MadTheme.cornerDpFor(IconShapeId.ADAPTIVE), 0.01f)
        assertEquals(12f, MadTheme.cornerDpFor(IconShapeId.ROUNDED_SQUARE), 0.01f)
        assertEquals(22f, MadTheme.cornerDpFor(IconShapeId.SQUIRCLE), 0.01f)
    }

    @Test
    fun defaultAppearanceUsesRoundedSquare() {
        assertEquals(IconShapeId.ROUNDED_SQUARE, AppearanceSettings().iconShape)
        assertEquals("", AppearanceSettings().iconStyleId)
    }

    @Test
    fun removedPlayfulShapeFallsBackToRoundedSquare() {
        val shape = runCatching { enumValueOf<IconShapeId>("STAR") }
            .getOrDefault(IconShapeId.ROUNDED_SQUARE)
        assertEquals(IconShapeId.ROUNDED_SQUARE, shape)
        assertEquals("", AppearanceSettings().iconStyleId)
    }

    @Test
    fun adaptiveIconShapeIsAvailable() {
        assertEquals(true, IconShapeId.entries.contains(IconShapeId.ADAPTIVE))
        assertEquals(0f, MadTheme.cornerDpFor(IconShapeId.ADAPTIVE), 0.01f)
        assertEquals(IconShapeId.ADAPTIVE, AppearanceSettings(iconShape = IconShapeId.ADAPTIVE).iconShape)
    }

    @Test
    fun actionBoxPillEditorDefaults() {
        val d = AppearanceSettings()
        assertEquals(1f, d.actionBoxFontScale, 0.01f)
        assertEquals(1f, d.actionBoxPillScale, 0.01f)
        assertEquals(ActionBoxFontId.DEFAULT, d.actionBoxFont)
        assertEquals(ActionBoxButtonsId.TRAILING, d.actionBoxButtons)
        val custom = AppearanceSettings(
            actionBoxFontScale = 1.2f,
            actionBoxPillScale = 0.9f,
            actionBoxFont = ActionBoxFontId.MONO,
            actionBoxButtons = ActionBoxButtonsId.SPLIT,
        )
        assertEquals(1.2f, custom.actionBoxFontScale, 0.01f)
        assertEquals(0.9f, custom.actionBoxPillScale, 0.01f)
        assertEquals(ActionBoxFontId.MONO, custom.actionBoxFont)
        assertEquals(ActionBoxButtonsId.SPLIT, custom.actionBoxButtons)
    }

    @Test
    fun madWindowsAreExperimentalAndOffByDefault() {
        assertEquals(false, AppearanceSettings().experimentalMadWindows)
        val json = AppearanceSettings.fromJson(null)
        assertEquals(false, json.experimentalMadWindows)
    }

    @Test
    fun navShortcutsAreExperimentalAndOffByDefault() {
        assertEquals(false, AppearanceSettings().experimentalNavShortcuts)
        assertEquals(false, AppearanceSettings.fromJson(null).experimentalNavShortcuts)
        assertEquals(false, AppearanceSettings(experimentalNavShortcuts = false).experimentalNavShortcuts)
        assertEquals(true, AppearanceSettings(experimentalNavShortcuts = true).experimentalNavShortcuts)
    }

    @Test
    fun madRecentsAreExperimentalAndOffByDefault() {
        assertEquals(false, AppearanceSettings().experimentalMadRecents)
        assertEquals(false, AppearanceSettings.fromJson(null).experimentalMadRecents)
        assertEquals(true, AppearanceSettings(experimentalMadRecents = true).experimentalMadRecents)
    }

    @Test
    fun widgetBadgesAreExperimentalAndOffByDefault() {
        assertEquals(false, AppearanceSettings().experimentalWidgetBadges)
        assertEquals(false, AppearanceSettings.fromJson(null).experimentalWidgetBadges)
        assertEquals(true, AppearanceSettings(experimentalWidgetBadges = true).experimentalWidgetBadges)
    }

    @Test
    fun socialBrowserAppsAreOffByDefault() {
        assertEquals(emptySet<String>(), AppearanceSettings().experimentalSocialBrowserApps)
        assertEquals(emptySet<String>(), AppearanceSettings.fromJson(null).experimentalSocialBrowserApps)
        assertEquals(
            setOf("x"),
            AppearanceSettings(experimentalSocialBrowserApps = setOf("x")).experimentalSocialBrowserApps,
        )
    }

    @Test
    fun transparencyAllowsFullyClear() {
        assertEquals(1f, AppearanceSettings().iconBgAlpha, 0.01f)
        assertEquals(false, AppearanceSettings().dockSecondRow)
        assertEquals(false, AppearanceSettings.fromJson(null).dockSecondRow)
        assertEquals(true, AppearanceSettings.fromJson("""{"dockSecondRow":true}""").dockSecondRow)
        val clear = AppearanceSettings(widgetAlpha = 0f, drawerAlpha = 0f, iconBgAlpha = 0f)
        assertEquals(0f, clear.widgetAlpha, 0.01f)
        assertEquals(0f, clear.drawerAlpha, 0.01f)
        assertEquals(0f, clear.iconBgAlpha, 0.01f)
        val dockEra = AppearanceSettings.fromJson("""{"iconBgArgb":4278223297,"iconBgAlpha":0.12}""")
        assertEquals(1f, dockEra.iconBgAlpha, 0.01f)
        val faded = AppearanceSettings.fromJson("""{"iconBgAlpha":0.4}""")
        assertEquals(0.4f, faded.iconBgAlpha, 0.01f)
    }

    @Test
    fun pinchOutDefaultsToSystemRecents() {
        assertEquals(GestureActionId.RECENTS, AppearanceSettings().gestures[GestureId.PINCH_OUT])
    }

    @Test
    fun webSearchDefaultsToDuckDuckGoWhenMissing() {
        assertEquals(WebSearchEngine.DUCKDUCKGO, AppearanceSettings().webSearchEngine)
        assertEquals(WebSearchEngine.DUCKDUCKGO, AppearanceSettings.fromJson(null).webSearchEngine)
        assertEquals(WebSearchEngine.DUCKDUCKGO, AppearanceSettings.fromJson("").webSearchEngine)
        assertEquals(WebSearchEngine.GOOGLE, AppearanceSettings(webSearchEngine = WebSearchEngine.GOOGLE).webSearchEngine)
        assertEquals(WebSearchEngine.BRAVE, AppearanceSettings(webSearchEngine = WebSearchEngine.BRAVE).webSearchEngine)
    }

    @Test
    fun actionBoxHowToStartsUnseen() {
        assertEquals(false, AppearanceSettings().actionBoxHowToSeen)
        assertEquals(false, AppearanceSettings.fromJson(null).actionBoxHowToSeen)
        assertEquals(true, AppearanceSettings(actionBoxHowToSeen = true).actionBoxHowToSeen)
    }

    @Test
    fun instantAnswersDefaultOn() {
        assertEquals(true, AppearanceSettings().instantAnswersEnabled)
        assertEquals(true, AppearanceSettings.fromJson(null).instantAnswersEnabled)
        assertEquals(true, AppearanceSettings.fromJson("").instantAnswersEnabled)
        assertEquals(false, AppearanceSettings(instantAnswersEnabled = false).instantAnswersEnabled)
        assertEquals(true, AppearanceSettings().instantAnswerCardsEnabled)
        assertEquals(
            false,
            AppearanceSettings(webSearchEngine = WebSearchEngine.GOOGLE).instantAnswerCardsEnabled,
        )
    }

    @Test
    fun instantAnswersOffWhenStoredEngineIsNotDuckDuckGo() {
        val google = AppearanceSettings(
            webSearchEngine = WebSearchEngine.GOOGLE,
            instantAnswersEnabled = true,
        )
        assertEquals(false, google.instantAnswerCardsEnabled)
        val bing = AppearanceSettings(
            webSearchEngine = WebSearchEngine.BING,
            instantAnswersEnabled = true,
        )
        assertEquals(false, bing.instantAnswerCardsEnabled)
        val ddgOff = AppearanceSettings(
            webSearchEngine = WebSearchEngine.DUCKDUCKGO,
            instantAnswersEnabled = false,
        )
        assertEquals(false, ddgOff.instantAnswerCardsEnabled)
    }

    @Test
    fun lightDarkFollowsSystemNightMode() {
        assertEquals(ThemeMode.SYSTEM, AppearanceSettings().themeMode)
        assertEquals(ThemeMode.SYSTEM, AppearanceSettings.fromJson(null).themeMode)
        assertEquals(ThemeMode.SYSTEM, AppearanceSettings.fromJson("").themeMode)
    }

    @Test
    fun palettesAreSystemNordOrWallpaper() {
        assertEquals(PaletteId.NORD, AppearanceSettings().palette)
        assertEquals(
            setOf("NORD", "NORD_LIGHT", "WALLPAPER", "MATERIAL", "STEAMPUNK"),
            PaletteId.entries.map { it.name }.toSet(),
        )
        assertEquals(PaletteId.NORD, AppearanceSettings.normalizePalette(PaletteId.NORD_LIGHT))
        assertEquals(PaletteId.MATERIAL, AppearanceSettings.normalizePalette(PaletteId.MATERIAL))
        assertEquals(PaletteId.WALLPAPER, AppearanceSettings.normalizePalette(PaletteId.WALLPAPER))
    }

    @Test
    fun landscapeRotateContentsOffByDefault() {
        assertEquals(false, AppearanceSettings().landscapeRotateContents)
        assertEquals(true, AppearanceSettings(landscapeRotateContents = true).landscapeRotateContents)
    }
}
