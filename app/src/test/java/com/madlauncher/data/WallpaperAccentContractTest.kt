package com.madlauncher.data

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperAccentContractTest {
    @Test
    fun wallpaperPaletteKeepsMadAccent() {
        val wp = WallpaperColorExtractor.WallpaperPalette(
            primary = Color(0xFF112233),
            secondary = Color(0xFF445566),
            tertiary = Color(0xFF778899),
            isDark = true,
        )
        val madAccent = Color(0xFF81A1C1)
        val colors = WallpaperColorExtractor.toPalette(wp, madAccent)
        assertEquals(madAccent, colors.primary)
        assertEquals(madAccent, colors.accent)
    }
}
