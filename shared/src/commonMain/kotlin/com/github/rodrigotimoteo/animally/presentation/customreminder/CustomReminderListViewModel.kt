package com.github.rodrigotimoteo.animally.presentation.customreminder

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.DeleteCustomReminderUseCase
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.GetCustomRemindersByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the custom reminder list screen.
 *
 * @param patientId The id of the patient whose custom reminders are listed.
 * @param getCustomRemindersByPatientUseCase Use case for loading the custom reminders.
 * @param deleteCustomReminderUseCase Use case for deactivating a custom reminder.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class CustomReminderListViewModel(
    private val patientId: Long,
    private val getCustomRemindersByPatientUseCase: GetCustomRemindersByPatientUseCase,
    private val deleteCustomReminderUseCase: DeleteCustomReminderUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(CustomReminderListUiState())
    val uiState: StateFlow<CustomReminderListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the custom reminder list for the patient, split into upcoming and overdue groups.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val reminders = getCustomRemindersByPatientUseCase(patientId)
                    reminders.partition { it.dueDate < today }
                }
            }.onSuccess { (overdue, upcoming) ->
                _uiState.update { it.copy(overdue = overdue, upcoming = upcoming, isLoading = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Deactivates the reminder with the given [reminderId] and reloads the list.
     */
    fun onDeleteClick(reminderId: Long) {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { deleteCustomReminderUseCase(reminderId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-custom-reminder screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditCustomReminder(patientId))

    /**
     * Navigates to the edit screen for the reminder with the given [reminderId].
     */
    fun onEditClick(reminderId: Long) = navigateTo(Route.AddEditCustomReminder(patientId, reminderId))

    /**
     * Navigates back to the previous destination.
     */
    fun onBack() = popBackStack()

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the custom reminder list.
 *
 * @param upcoming The active reminders due today or later, ordered by due date.
 * @param overdue The active reminders due before today.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class CustomReminderListUiState(
    val upcoming: List<CustomReminder> = emptyList(),
    val overdue: List<CustomReminder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
