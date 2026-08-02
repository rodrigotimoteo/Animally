package com.github.rodrigotimoteo.animally.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Animally Material 3 theme.
 *
 * Resolves the color scheme from three inputs:
 * 1. [themeMode] — user preference (light/dark/system).
 * 2. [dynamicColor] — when `true` on Android 12+, Material You palette from the wallpaper.
 * 3. Static curated palette (forest green / amber / sage) as the default and iOS fallback.
 *
 * Typography uses the system default font family with `sp` sizing so accessibility font-scale
 * is respected.
 *
 * @param themeMode The persisted user preference for light/dark/system.
 * @param dynamicColor Whether to attempt Material You dynamic color (Android 12+ only).
 * @param content The composable tree rendered inside the theme.
 */
@Composable
fun AnimallyTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    val colorScheme = resolveColorScheme(darkTheme, dynamicColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnimallyTypography,
        content = content,
    )
}

@Composable
private fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean =
    when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

@Composable
private fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
) = when {
    dynamicColor -> dynamicColorScheme(darkTheme)
    else -> null
} ?: if (darkTheme) animallyDarkColorScheme else animallyLightColorScheme
