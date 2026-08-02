package com.github.rodrigotimoteo.animally.presentation.theme

/**
 * Theme mode preference persisted across app launches.
 *
 * @property label Human-readable label shown in the settings UI.
 */
enum class ThemeMode(
    val label: String,
) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}
