package com.madlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.LanguageOverlay
import com.madlauncher.data.WallpaperColorExtractor
import com.madlauncher.ui.LanguageOverlayContextWrapper
import com.madlauncher.ui.isRtlLanguageOverlay
import java.io.File

@Composable
fun MadLauncherTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    refreshKey: Int = 0,
    wallpaperPalette: WallpaperColorExtractor.WallpaperPalette? = null,
    content: @Composable () -> Unit,
) {
    val overlayRev = LanguageOverlay.revision.intValue
    val baseContext = LocalContext.current
    val overlayContext = remember(baseContext) { LanguageOverlayContextWrapper(baseContext) }
    val layoutDirection = if (isRtlLanguageOverlay(LanguageOverlay.selectedFile)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val fontFamily = remember(appearance.fontPath, refreshKey, overlayRev) {
        val path = appearance.fontPath
        if (path.isBlank()) FontFamily.Default // system default (respects OEM bold / weight slider)
        else {
            val file = File(path)
            if (file.exists()) runCatching { FontFamily(Font(file)) }.getOrDefault(FontFamily.Default)
            else FontFamily.Default
        }
    }
    val spec = MadTheme.resolve(appearance, fontFamily, wallpaperPalette)
    val scheme = if (spec.useLightSystemBars) {
        lightColorScheme(
            primary = spec.colors.primary,
            onPrimary = spec.colors.fg,
            secondary = spec.colors.secondary,
            onSecondary = spec.colors.fg,
            background = spec.colors.bg,
            onBackground = spec.colors.fg,
            surface = spec.colors.card,
            onSurface = spec.colors.fg,
            surfaceVariant = spec.colors.secondary,
            onSurfaceVariant = spec.colors.muted,
            outline = spec.colors.border,
            error = spec.colors.red,
            onError = spec.colors.fg,
        )
    } else {
        darkColorScheme(
            primary = spec.colors.primary,
            onPrimary = spec.colors.bg,
            secondary = spec.colors.secondary,
            onSecondary = spec.colors.fg,
            background = spec.colors.bg,
            onBackground = spec.colors.fg,
            surface = spec.colors.card,
            onSurface = spec.colors.fg,
            surfaceVariant = spec.colors.secondary,
            onSurfaceVariant = spec.colors.muted,
            outline = spec.colors.border,
            error = spec.colors.red,
            onError = spec.colors.fg,
        )
    }
    val typography = MaterialTheme.typography.let { base ->
        base.copy(
            bodyLarge = base.bodyLarge.merge(TextStyle(fontFamily = fontFamily)),
            bodyMedium = base.bodyMedium.merge(TextStyle(fontFamily = fontFamily)),
            bodySmall = base.bodySmall.merge(TextStyle(fontFamily = fontFamily)),
            titleLarge = base.titleLarge.merge(TextStyle(fontFamily = fontFamily)),
            titleMedium = base.titleMedium.merge(TextStyle(fontFamily = fontFamily)),
            titleSmall = base.titleSmall.merge(TextStyle(fontFamily = fontFamily)),
            labelLarge = base.labelLarge.merge(TextStyle(fontFamily = fontFamily)),
        )
    }
    CompositionLocalProvider(
        LocalMadTheme provides spec,
        LocalContext provides overlayContext,
        LocalLayoutDirection provides layoutDirection,
    ) {
        key(refreshKey, overlayRev) {
            MaterialTheme(colorScheme = scheme, typography = typography, content = content)
        }
    }
}
