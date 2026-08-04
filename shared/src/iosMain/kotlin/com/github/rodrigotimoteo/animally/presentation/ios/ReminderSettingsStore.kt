@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsUiState
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ReminderSettingsViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ReminderSettingsStore")
class ReminderSettingsStore(
    private val viewModel: ReminderSettingsViewModel,
) {
    /** Observable state of the reminders section of the settings screen. */
    val state: NativeFlow<ReminderSettingsUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Enables or disables scheduling of reminder notifications. */
    fun setRemindersEnabled(enabled: Boolean) {
        viewModel.setRemindersEnabled(enabled)
    }

    /** Collects all due reminders and schedules notifications for them. */
    fun checkRemindersNow() {
        viewModel.checkRemindersNow()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
