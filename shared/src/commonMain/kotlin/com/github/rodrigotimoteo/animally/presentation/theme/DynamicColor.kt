package com.github.rodrigotimoteo.animally.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns the platform dynamic color scheme for the given [darkTheme] flag, or `null` when dynamic
 * color is unavailable (Android < 12, iOS).
 *
 * Material You dynamic color extracts a palette from the user's wallpaper on Android 12+.
 * On platforms without this capability, [AnimallyTheme] falls back to the static curated palette.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
