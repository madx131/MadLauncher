package com.madlauncher.data

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.madlauncher.ui.theme.MadColors
import com.madlauncher.ui.theme.NordGreen
import com.madlauncher.ui.theme.NordYellow
import com.madlauncher.ui.theme.NordOrange
import com.madlauncher.ui.theme.NordRed
import com.madlauncher.ui.theme.NordPurple
import com.madlauncher.ui.theme.NordBlue

object WallpaperColorExtractor {

    data class WallpaperPalette(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val isDark: Boolean,
    )

    fun extract(context: Context): WallpaperPalette? {
        if (Build.VERSION.SDK_INT < 27) return null
        val wm = WallpaperManager.getInstance(context)
        val colors = runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
            ?: return null
        val primary = colors.primaryColor?.toArgb()?.let { Color(it) } ?: return null
        val secondary = colors.secondaryColor?.toArgb()?.let { Color(it) } ?: primary.copy(alpha = 0.7f)
        val tertiary = colors.tertiaryColor?.toArgb()?.let { Color(it) } ?: secondary.copy(alpha = 0.5f)
        val lum = luminance(primary)
        return WallpaperPalette(primary, secondary, tertiary, isDark = lum < 0.5f)
    }

    fun toPalette(wp: WallpaperPalette, madAccent: Color? = null): MadColors {
        val base = if (wp.isDark) darkFromWallpaper(wp) else lightFromWallpaper(wp)
        val accent = madAccent?.takeUnless { it == Color.Unspecified } ?: return base
        // Wallpaper seeds surfaces; MAD accent stays the brand primary.
        return base.copy(primary = accent, accent = accent)
    }

    private fun darkFromWallpaper(wp: WallpaperPalette): MadColors {
        val bg = darken(wp.primary, 0.15f)
        val card = darken(wp.primary, 0.22f)
        val fg = Color.White
        return MadColors(
            bg = bg,
            fg = fg,
            card = card,
            primary = wp.secondary,
            secondary = darken(wp.secondary, 0.35f),
            muted = fg.copy(alpha = 0.6f),
            accent = wp.tertiary,
            border = wp.secondary.copy(alpha = 0.3f),
            green = NordGreen,
            yellow = NordYellow,
            orange = NordOrange,
            red = NordRed,
            purple = NordPurple,
            blue = NordBlue,
        )
    }

    private fun lightFromWallpaper(wp: WallpaperPalette): MadColors {
        val bg = lighten(wp.primary, 0.85f)
        val card = lighten(wp.primary, 0.78f)
        val fg = Color(0xFF1A1A1A)
        return MadColors(
            bg = bg,
            fg = fg,
            card = card,
            primary = wp.secondary,
            secondary = lighten(wp.secondary, 0.7f),
            muted = fg.copy(alpha = 0.55f),
            accent = wp.tertiary,
            border = wp.secondary.copy(alpha = 0.25f),
            green = NordGreen,
            yellow = NordYellow,
            orange = NordOrange,
            red = NordRed,
            purple = NordPurple,
            blue = NordBlue,
        )
    }

    private fun luminance(c: Color): Float {
        return 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    }

    private fun darken(c: Color, target: Float): Color {
        val factor = target / luminance(c).coerceAtLeast(0.01f)
        return Color(
            red = (c.red * factor).coerceIn(0f, 1f),
            green = (c.green * factor).coerceIn(0f, 1f),
            blue = (c.blue * factor).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }

    private fun lighten(c: Color, target: Float): Color {
        val lum = luminance(c).coerceAtLeast(0.01f)
        val factor = target / lum
        return Color(
            red = (c.red * factor).coerceIn(0f, 1f),
            green = (c.green * factor).coerceIn(0f, 1f),
            blue = (c.blue * factor).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
}
