package com.github.rodrigotimoteo.animally.presentation.reminder

import platform.Foundation.NSUserDefaults

/** iOS reminder preference backed by the standard user defaults suite. */
private class IosReminderPreferenceStore : ReminderPreferenceStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun isEnabled(): Boolean =
        if (defaults.objectForKey(REMINDERS_ENABLED_KEY) == null) {
            true
        } else {
            defaults.boolForKey(REMINDERS_ENABLED_KEY)
        }

    override fun setEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = REMINDERS_ENABLED_KEY)
    }
}

actual fun createPlatformReminderPreferenceStore(): ReminderPreferenceStore = IosReminderPreferenceStore()

private const val REMINDERS_ENABLED_KEY = "reminders_enabled"
