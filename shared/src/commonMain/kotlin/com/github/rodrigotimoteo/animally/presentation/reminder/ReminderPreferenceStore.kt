package com.github.rodrigotimoteo.animally.presentation.reminder

/** Small platform boundary for the user's reminder-notification preference. */
interface ReminderPreferenceStore {
    /** Returns the persisted reminder setting, defaulting to enabled. */
    fun isEnabled(): Boolean

    /** Persists whether reminder notifications should be scheduled. */
    fun setEnabled(enabled: Boolean)
}

/** Creates the platform-backed reminder preference store. */
expect fun createPlatformReminderPreferenceStore(): ReminderPreferenceStore
