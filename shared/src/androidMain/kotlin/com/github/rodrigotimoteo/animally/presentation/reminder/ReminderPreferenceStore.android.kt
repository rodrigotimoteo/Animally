package com.github.rodrigotimoteo.animally.presentation.reminder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.rodrigotimoteo.animally.di.infra.appContext

/** Android reminder preference backed by the app's existing preferences file. */
private class AndroidReminderPreferenceStore(
    context: Context,
) : ReminderPreferenceStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = preferences.getBoolean(REMINDERS_ENABLED_KEY, true)

    override fun setEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(REMINDERS_ENABLED_KEY, enabled) }
    }
}

actual fun createPlatformReminderPreferenceStore(): ReminderPreferenceStore = AndroidReminderPreferenceStore(appContext)

private const val PREFERENCES_NAME = "animally_preferences"
private const val REMINDERS_ENABLED_KEY = "reminders_enabled"
