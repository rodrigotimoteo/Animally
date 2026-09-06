package com.github.rodrigotimoteo.animally.presentation.reminder

import java.util.prefs.Preferences

/** Desktop reminder preference backed by the JVM user preferences node. */
private class DesktopReminderPreferenceStore : ReminderPreferenceStore {
    private val preferences = Preferences.userRoot().node(PREFERENCES_NODE)

    override fun isEnabled(): Boolean = preferences.getBoolean(REMINDERS_ENABLED_KEY, true)

    override fun setEnabled(enabled: Boolean) {
        preferences.putBoolean(REMINDERS_ENABLED_KEY, enabled)
    }
}

actual fun createPlatformReminderPreferenceStore(): ReminderPreferenceStore = DesktopReminderPreferenceStore()

private const val PREFERENCES_NODE = "animally"
private const val REMINDERS_ENABLED_KEY = "reminders_enabled"
