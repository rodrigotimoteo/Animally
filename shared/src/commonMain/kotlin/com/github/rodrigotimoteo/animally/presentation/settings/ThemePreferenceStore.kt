package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.presentation.theme.AccentColor
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode

/**
 * Persists the user's theme mode preference across app launches.
 *
 * Platform implementations back this with SharedPreferences (Android) or NSUserDefaults (iOS).
 * Exposed as an interface so tests can substitute an in-memory fake.
 */
interface ThemePreferenceStore {
    /** Returns the persisted [ThemeMode], defaulting to [ThemeMode.SYSTEM] when unset. */
    fun getThemeMode(): ThemeMode

    /** Persists [mode] as the user's theme preference. */
    fun setThemeMode(mode: ThemeMode)

    /** Returns the persisted accent, defaulting to [AccentColor.FOREST]. */
    fun getAccentColor(): AccentColor

    /** Persists [accent] as the user's brand accent. */
    fun setAccentColor(accent: AccentColor)
}

/** Key used by platform implementations to store the theme mode ordinal. */
const val THEME_MODE_PREF_KEY = "animally_theme_mode"

/** Key used by platform implementations to store the stable accent ID. */
const val THEME_ACCENT_COLOR_PREF_KEY = "animally_accent_color"

/**
 * Creates the platform-specific [ThemePreferenceStore].
 *
 * Android actual uses SharedPreferences; iOS actual uses NSUserDefaults.
 */
expect fun createPlatformThemePreferenceStore(): ThemePreferenceStore
