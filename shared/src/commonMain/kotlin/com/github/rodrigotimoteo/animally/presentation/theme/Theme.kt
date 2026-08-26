package com.github.rodrigotimoteo.animally.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Animally Material 3 theme.
 *
 * Resolves the color scheme from three inputs:
 * 1. [themeMode] — user preference (light/dark/system).
 * 2. [accentColor] — the user's selected brand accent.
 * 3. [dynamicColor] — optional Material You colors for the remaining surfaces.
 *
 * Typography uses the system default font family with `sp` sizing so accessibility font-scale
 * is respected.
 *
 * @param themeMode The persisted user preference for light/dark/system.
 * @param accentColor The persisted brand accent.
 * @param dynamicColor Whether to attempt Material You dynamic color (Android 12+ only).
 * @param content The composable tree rendered inside the theme.
 */
@Composable
fun AnimallyTheme(
    themeMode: ThemeMode,
    accentColor: AccentColor = AccentColor.FOREST,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    val colorScheme = resolveColorScheme(darkTheme, accentColor, dynamicColor)

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
    accentColor: AccentColor,
    dynamicColor: Boolean,
): androidx.compose.material3.ColorScheme {
    val baseScheme =
        when {
            dynamicColor -> dynamicColorScheme(darkTheme)
            else -> null
        } ?: if (darkTheme) animallyDarkColorScheme else animallyLightColorScheme
    return baseScheme.withAccent(accentColor, darkTheme)
}
