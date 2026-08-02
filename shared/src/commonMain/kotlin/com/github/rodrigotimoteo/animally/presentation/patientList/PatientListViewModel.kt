package com.github.rodrigotimoteo.animally.presentation.patientList

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.DeletePatientUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.PatientHasRecordsException
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
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named

/**
 * View model for the patient list screen.
 *
 * @param getPatientListUseCase Use case for loading the patient list.
 * @param deletePatientUseCase Use case for soft-deleting patients.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
@KoinViewModel
class PatientListViewModel(
    private val getPatientListUseCase: GetPatientListUseCase,
    private val deletePatientUseCase: DeletePatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(PatientListUiState())
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    init {
        loadPatients()
    }

    /**
     * Reloads the patient list from the repository.
     */
    fun loadPatients() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getPatientListUseCase() } }
                .onSuccess { patients ->
                    _uiState.update { it.copy(patients = patients, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the detail screen for the patient with the given [patientId].
     */
    fun onPatientClick(patientId: Long) = navigateTo(Route.PatientDetail(patientId))

    /**
     * Navigates to the add-patient screen.
     */
    fun onAddClick() = navigateTo(Route.AddEditPatient())

    /**
     * Soft-deletes the patient with the given [patientId].
     *
     * Deletion is blocked while the patient still has active linked records.
     */
    fun onDeleteClick(patientId: Long) {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { deletePatientUseCase(patientId) } }
                .onSuccess { loadPatients() }
                .onFailure { error ->
                    val message = if (error is PatientHasRecordsException) error.message else "Failed to delete patient"
                    _uiState.update { it.copy(errorMessage = message) }
                }
        }
    }

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the patient list screen.
 *
 * @param patients The currently loaded patients.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class PatientListUiState(
    val patients: List<Patient> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
