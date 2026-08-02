package com.github.rodrigotimoteo.animally.presentation.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.rodrigotimoteo.animally.di.infra.appContext
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode

/**
 * Android [ThemePreferenceStore] backed by [SharedPreferences].
 *
 * Reads and writes the theme mode ordinal to `animally_preferences`.
 *
 * @param context Application or app-context providing access to shared preferences.
 */
class AndroidThemePreferenceStore(
    context: Context,
) : ThemePreferenceStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getThemeMode(): ThemeMode {
        val ordinal = prefs.getInt(THEME_MODE_PREF_KEY, ThemeMode.SYSTEM.ordinal)
        return entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putInt(THEME_MODE_PREF_KEY, mode.ordinal) }
    }

    private companion object {
        const val PREFS_NAME = "animally_preferences"
        val entries = ThemeMode.entries
    }
}

/** Android [ThemePreferenceStore] factory backed by [SharedPreferences]. */
actual fun createPlatformThemePreferenceStore(): ThemePreferenceStore = AndroidThemePreferenceStore(appContext)
