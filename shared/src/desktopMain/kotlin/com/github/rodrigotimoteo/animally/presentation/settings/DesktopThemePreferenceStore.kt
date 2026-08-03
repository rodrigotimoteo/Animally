package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import java.util.prefs.Preferences

/**
 * Desktop [ThemePreferenceStore] backed by the JVM [Preferences] API.
 *
 * Reads and writes the theme mode ordinal to the user-level `animally` node.
 */
class DesktopThemePreferenceStore : ThemePreferenceStore {
    private val prefs: Preferences = Preferences.userRoot().node(NODE_NAME)

    override fun getThemeMode(): ThemeMode {
        val ordinal = prefs.getInt(THEME_MODE_PREF_KEY, ThemeMode.SYSTEM.ordinal)
        return entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.putInt(THEME_MODE_PREF_KEY, mode.ordinal)
    }

    private companion object {
        const val NODE_NAME = "animally"
        val entries = ThemeMode.entries
    }
}

/** Desktop [ThemePreferenceStore] factory backed by the JVM [Preferences] API. */
actual fun createPlatformThemePreferenceStore(): ThemePreferenceStore = DesktopThemePreferenceStore()
