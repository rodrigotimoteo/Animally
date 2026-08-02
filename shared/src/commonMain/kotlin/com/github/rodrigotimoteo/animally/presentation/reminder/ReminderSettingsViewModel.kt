package com.github.rodrigotimoteo.animally.presentation.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.notification.NotificationPermissionController
import com.github.rodrigotimoteo.animally.domain.notification.NotificationScheduler
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetDentistryRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetVaccinationRemindersUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the reminders section of the settings screen.
 *
 * Runs the vaccination and dentistry reminder use cases on demand and hands the resulting
 * reminders to the platform notification scheduler. Scheduling is gated by [ReminderSettingsUiState.remindersEnabled],
 * which only stays enabled while notification permission is granted.
 *
 * @param getVaccinationRemindersUseCase Use case collecting vaccination reminders.
 * @param getDentistryRemindersUseCase Use case collecting dentistry reminders.
 * @param ioDispatcher Dispatcher for blocking database work.
 * @param notificationPermissionController Platform notification permission access.
 */
@KoinViewModel
class ReminderSettingsViewModel(
    private val getVaccinationRemindersUseCase: GetVaccinationRemindersUseCase,
    private val getDentistryRemindersUseCase: GetDentistryRemindersUseCase,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val notificationPermissionController: NotificationPermissionController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderSettingsUiState())
    val uiState: StateFlow<ReminderSettingsUiState> = _uiState.asStateFlow()

    private val notificationScheduler = NotificationScheduler()

    init {
        viewModelScope.launch {
            val granted = withContext(ioDispatcher) { notificationPermissionController.isGranted() }
            _uiState.update {
                it.copy(
                    notificationsEnabled = granted,
                    remindersEnabled = it.remindersEnabled && granted,
                )
            }
        }
    }

    /**
     * Enables or disables scheduling of reminders by the platform notification scheduler.
     *
     * Enabling while permission is not granted first requests it; a denied request keeps the
     * toggle off and surfaces [ReminderSettingsUiState.permissionMessage].
     */
    fun setRemindersEnabled(enabled: Boolean) {
        if (enabled && _uiState.value.notificationsEnabled != true) {
            requestNotificationPermission()
            return
        }
        _uiState.update { it.copy(remindersEnabled = enabled, permissionMessage = null) }
    }

    private fun requestNotificationPermission() {
        _uiState.update { it.copy(isPermissionRequesting = true, permissionMessage = null) }
        viewModelScope.launch {
            val granted = withContext(ioDispatcher) { notificationPermissionController.request() }
            _uiState.update {
                it.copy(
                    notificationsEnabled = granted,
                    remindersEnabled = granted,
                    isPermissionRequesting = false,
                    permissionMessage = if (granted) null else NOTIFICATIONS_DISABLED_MESSAGE,
                )
            }
        }
    }

    /**
     * Collects all vaccination and dentistry reminders and schedules notifications for them.
     *
     * When reminders are disabled the reminders are still counted but not scheduled.
     */
    fun checkRemindersNow() {
        _uiState.update { it.copy(isChecking = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    getVaccinationRemindersUseCase(today) + getDentistryRemindersUseCase(today)
                }
            }.onSuccess { reminders ->
                if (_uiState.value.remindersEnabled) {
                    reminders.forEach { notificationScheduler.scheduleReminder(it) }
                }
                _uiState.update {
                    it.copy(isChecking = false, lastCheckedCount = reminders.size)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isChecking = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private const val NOTIFICATIONS_DISABLED_MESSAGE =
    "Notifications are disabled for this app. Enable them in system settings to receive reminders."

/**
 * UI state for the reminders section of the settings screen.
 *
 * @param remindersEnabled Whether reminder notifications are scheduled.
 * @param notificationsEnabled Whether the platform has authorized notifications, or `null` before the first check.
 * @param isPermissionRequesting Whether a permission request is in flight.
 * @param permissionMessage Message explaining why reminders stay disabled, or `null`.
 * @param isChecking Whether reminders are currently being collected.
 * @param lastCheckedCount Number of reminders found by the last check, or `null` before the first check.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ReminderSettingsUiState(
    val remindersEnabled: Boolean = true,
    val notificationsEnabled: Boolean? = null,
    val isPermissionRequesting: Boolean = false,
    val permissionMessage: String? = null,
    val isChecking: Boolean = false,
    val lastCheckedCount: Int? = null,
    val errorMessage: String? = null,
)
