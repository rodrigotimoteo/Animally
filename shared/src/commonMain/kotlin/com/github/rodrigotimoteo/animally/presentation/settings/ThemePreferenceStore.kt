package com.github.rodrigotimoteo.animally.presentation.settings

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
}

/** Key used by platform implementations to store the theme mode ordinal. */
const val THEME_MODE_PREF_KEY = "animally_theme_mode"

/**
 * Creates the platform-specific [ThemePreferenceStore].
 *
 * Android actual uses SharedPreferences; iOS actual uses NSUserDefaults.
 */
expect fun createPlatformThemePreferenceStore(): ThemePreferenceStore
