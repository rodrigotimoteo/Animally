package com.github.rodrigotimoteo.animally.presentation.patientDetail

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
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
import org.koin.core.annotation.Named

/**
 * View model for the patient detail screen.
 *
 * @param patientId The id of the patient to display.
 * @param getPatientDetailUseCase Use case for loading the patient.
 * @param getOwnerDetailUseCase Use case for loading the linked owner's name.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class PatientDetailViewModel(
    private val patientId: Long,
    private val getPatientDetailUseCase: GetPatientDetailUseCase,
    private val getOwnerDetailUseCase: GetOwnerDetailUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(PatientDetailUiState())
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the patient and, when assigned, the name of its owner.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    val patient = getPatientDetailUseCase(patientId)
                    val owner: Owner? = patient?.ownerId?.let { getOwnerDetailUseCase(it) }
                    patient to owner
                }
            }.onSuccess { (patient, owner) ->
                _uiState.update {
                    it.copy(
                        patient = patient,
                        ownerName = owner?.name,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Navigates back to the previous destination.
     */
    fun onBack() = popBackStack()

    /**
     * Navigates to the edit screen for the current patient.
     */
    fun onEditClick() {
        _uiState.value.patient?.let { navigateTo(Route.AddEditPatient(it.id)) }
    }

    /**
     * Navigates to the anamnese add/edit screen for the current patient.
     */
    fun onAnamneseClick() {
        navigateTo(Route.AddEditAnamnese(patientId))
    }

    /**
     * Navigates to the timeline feed for the current patient.
     */
    fun onTimelineClick() {
        navigateTo(Route.Timeline(patientId))
    }

    /**
     * Navigates to the custom reminders list for the current patient.
     */
    fun onCustomRemindersClick() {
        navigateTo(Route.CustomReminderList(patientId))
    }

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the patient detail screen.
 *
 * @param patient The loaded patient, or `null` when not found.
 * @param ownerName The name of the linked owner, or `null` when unassigned.
 * @param isLoading Whether the data is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class PatientDetailUiState(
    val patient: Patient? = null,
    val ownerName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
