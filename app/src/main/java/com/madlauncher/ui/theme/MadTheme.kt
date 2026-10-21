package com.madlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.IconShapeId
import com.madlauncher.data.PaletteId
import com.madlauncher.data.WallpaperColorExtractor

data class MadColors(
    val bg: Color,
    val fg: Color,
    val card: Color,
    val primary: Color,
    val secondary: Color,
    val muted: Color,
    val accent: Color,
    val border: Color,
    val green: Color,
    val yellow: Color,
    val orange: Color,
    val red: Color,
    val purple: Color,
    val blue: Color,
)

data class MadThemeSpec(
    val colors: MadColors,
    val windowAlpha: Float,
    val drawerAlpha: Float,
    val dockAlpha: Float,
    val widgetAlpha: Float,
    val newsAlpha: Float,
    val iconBgAlpha: Float,
    val blurEnabled: Boolean,
    val blurAllScreens: Boolean,
    val iconShape: IconShapeId,
    val iconCornerDp: Float,
    val fontFamily: FontFamily,
    val useLightSystemBars: Boolean,
)

val LocalMadTheme = staticCompositionLocalOf {
    MadThemeSpec(
        colors = MadColors(
            bg = NordBg,
            fg = NordFg,
            card = NordCard,
            primary = NordPrimary,
            secondary = NordSecondary,
            muted = NordMuted,
            accent = NordAccent,
            border = NordBorder,
            green = NordGreen,
            yellow = NordYellow,
            orange = NordOrange,
            red = NordRed,
            purple = NordPurple,
            blue = NordBlue,
        ),
        windowAlpha = 0.97f,
        drawerAlpha = 0.96f,
        dockAlpha = 0.88f,
        widgetAlpha = 0.88f,
        newsAlpha = 0.92f,
        iconBgAlpha = 1f,
        blurEnabled = false,
        blurAllScreens = false,
        iconShape = IconShapeId.ROUNDED_SQUARE,
        iconCornerDp = 12f,
        fontFamily = FontFamily.Default,
        useLightSystemBars = false,
    )
}

object MadTheme {
    @Composable
    fun resolve(
        settings: AppearanceSettings,
        fontFamily: FontFamily = FontFamily.Default,
        wallpaperPalette: WallpaperColorExtractor.WallpaperPalette? = null,
    ): MadThemeSpec {
        val dark = isSystemInDarkTheme()
        val palette = when {
            settings.palette == PaletteId.MATERIAL -> androidPalette(dark)
            settings.palette == PaletteId.STEAMPUNK -> steampunkPalette(dark, settings.accentColor)
            settings.palette == PaletteId.WALLPAPER && wallpaperPalette != null ->
                WallpaperColorExtractor.toPalette(wallpaperPalette, settings.accentColor)
            dark -> darkPalette(settings.accentColor)
            else -> lightPalette(settings.accentColor)
        }
        return MadThemeSpec(
            colors = palette,
            windowAlpha = settings.windowAlpha.coerceIn(0f, 1f),
            drawerAlpha = settings.drawerAlpha.coerceIn(0f, 1f),
            dockAlpha = settings.dockAlpha.coerceIn(0f, 1f),
            widgetAlpha = settings.widgetAlpha.coerceIn(0f, 1f),
            newsAlpha = settings.newsAlpha.coerceIn(0f, 1f),
            iconBgAlpha = settings.iconBgAlpha.coerceIn(0f, 1f),
            blurEnabled = false,
            blurAllScreens = false,
            iconShape = settings.iconShape,
            iconCornerDp = cornerDpFor(settings.iconShape),
            fontFamily = fontFamily,
            useLightSystemBars = !dark,
        )
    }

    fun cornerDpFor(shape: IconShapeId): Float = when (shape) {
        IconShapeId.ADAPTIVE -> 0f
        IconShapeId.ROUNDED_SQUARE -> 12f
        IconShapeId.SQUARE -> 0f
        IconShapeId.SQUIRCLE -> 22f
        IconShapeId.CIRCLE -> 999f
        IconShapeId.TEARDROP -> 50f
        IconShapeId.HEXAGON,
        IconShapeId.DIAMOND -> 0f
    }

    fun iconClip(shape: IconShapeId, cornerDp: Float): Shape = IconShapes.of(shape, cornerDp)

    /** Material 3 light/dark — not Nord. Used by standalone Action Box. */
    private fun androidPalette(dark: Boolean): MadColors {
        return if (dark) {
            MadColors(
                bg = Color(0xFF1C1B1F),
                fg = Color(0xFFE6E1E5),
                card = Color(0xFF2B2930),
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFF49454F),
                muted = Color(0xFFCAC4D0),
                accent = Color(0xFFD0BCFF),
                border = Color(0xFF938F99),
                green = Color(0xFF81C995),
                yellow = Color(0xFFFDD663),
                orange = Color(0xFFF4B183),
                red = Color(0xFFF2B8B5),
                purple = Color(0xFFE8DEF8),
                blue = Color(0xFFA8C7FA),
            )
        } else {
            MadColors(
                bg = Color(0xFFFFFBFE),
                fg = Color(0xFF1C1B1F),
                card = Color(0xFFF3EDF7),
                primary = Color(0xFF6750A4),
                secondary = Color(0xFFE7E0EC),
                muted = Color(0xFF49454F),
                accent = Color(0xFF6750A4),
                border = Color(0xFF79747E),
                green = Color(0xFF386A20),
                yellow = Color(0xFFBB8B00),
                orange = Color(0xFFC93400),
                red = Color(0xFFB3261E),
                purple = Color(0xFF625B71),
                blue = Color(0xFF0B57D0),
            )
        }
    }

    private fun darkPalette(accent: Color) = MadColors(
        bg = NordBg,
        fg = NordFg,
        card = NordCard,
        primary = accent,
        secondary = NordSecondary,
        muted = NordMuted,
        accent = accent.copy(alpha = 0.92f).let { if (it == Color.Unspecified) NordAccent else it },
        border = NordBorder,
        green = NordGreen,
        yellow = NordYellow,
        orange = NordOrange,
        red = NordRed,
        purple = NordPurple,
        blue = NordBlue,
    )

    private fun steampunkPalette(dark: Boolean, accent: Color): MadColors {
        val metal = if (accent == Color.Unspecified || accent == NordPrimary) {
            Color(0xFFC4A35A)
        } else {
            accent
        }
        return if (dark) {
            MadColors(
                bg = Color(0xFF1C1410),
                fg = Color(0xFFF3E6C9),
                card = Color(0xFF2F241C),
                primary = metal,
                secondary = Color(0xFF3D2E22),
                muted = Color(0xFFB8A48A),
                accent = metal.copy(alpha = 0.92f),
                border = Color(0xFF6B5428),
                green = Color(0xFF7A9E6A),
                yellow = Color(0xFFE6C35C),
                orange = Color(0xFFD4894A),
                red = Color(0xFFB54A3A),
                purple = Color(0xFF8B6B8A),
                blue = Color(0xFF6A8BA8),
            )
        } else {
            MadColors(
                bg = Color(0xFFE8D9B8),
                fg = Color(0xFF2A1F14),
                card = Color(0xFFF3E6C9),
                primary = metal,
                secondary = Color(0xFFD9C8A0),
                muted = Color(0xFF6B5428),
                accent = metal,
                border = Color(0xFF8A7340),
                green = Color(0xFF4E6B40),
                yellow = Color(0xFF8B6914),
                orange = Color(0xFFA85A22),
                red = Color(0xFF8B3228),
                purple = Color(0xFF6B4A68),
                blue = Color(0xFF3D5A73),
            )
        }
    }

    private fun lightPalette(accent: Color) = MadColors(
        bg = Color(0xFFECEFF4),
        fg = Color(0xFF2E3440),
        card = Color(0xFFE5E9F0),
        primary = accent,
        secondary = Color(0xFFD8DEE9),
        muted = Color(0xFF4C566A),
        accent = accent,
        border = Color(0xFFCBD5E1),
        green = NordGreen,
        yellow = NordYellow,
        orange = NordOrange,
        red = NordRed,
        purple = NordPurple,
        blue = NordBlue,
    )

}

private fun MadColors.copy(
    bg: Color = this.bg,
    fg: Color = this.fg,
    card: Color = this.card,
    primary: Color = this.primary,
    secondary: Color = this.secondary,
    muted: Color = this.muted,
    accent: Color = this.accent,
    border: Color = this.border,
    green: Color = this.green,
    yellow: Color = this.yellow,
    orange: Color = this.orange,
    red: Color = this.red,
    purple: Color = this.purple,
    blue: Color = this.blue,
) = MadColors(bg, fg, card, primary, secondary, muted, accent, border, green, yellow, orange, red, purple, blue)
