package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import platform.Foundation.NSUserDefaults

/**
 * iOS [ThemePreferenceStore] backed by [NSUserDefaults].
 *
 * Reads and writes the theme mode ordinal to the standard user defaults suite.
 */
class IosThemePreferenceStore : ThemePreferenceStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    override fun getThemeMode(): ThemeMode {
        val ordinal = defaults.integerForKey(THEME_MODE_PREF_KEY).toInt()
        return entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    override fun setThemeMode(mode: ThemeMode) {
        defaults.setInteger(mode.ordinal.toLong(), forKey = THEME_MODE_PREF_KEY)
    }

    private companion object {
        val entries = ThemeMode.entries
    }
}

/** iOS [ThemePreferenceStore] factory backed by [NSUserDefaults]. */
actual fun createPlatformThemePreferenceStore(): ThemePreferenceStore = IosThemePreferenceStore()
