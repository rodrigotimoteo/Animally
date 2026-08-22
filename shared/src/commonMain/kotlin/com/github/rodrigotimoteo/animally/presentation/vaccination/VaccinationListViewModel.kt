package com.github.rodrigotimoteo.animally.presentation.vaccination

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.DeleteVaccinationUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.GetVaccinationsByPatientUseCase
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
 * View model for the vaccination list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose vaccinations are listed.
 * @param getVaccinationsByPatientUseCase Use case for loading the vaccinations.
 * @param deleteVaccinationUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class VaccinationListViewModel(
    private val patientId: Long,
    private val getVaccinationsByPatientUseCase: GetVaccinationsByPatientUseCase,
    private val deleteVaccinationUseCase: DeleteVaccinationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(VaccinationListUiState())
    val uiState: StateFlow<VaccinationListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the vaccination list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getVaccinationsByPatientUseCase(patientId) } }
                .onSuccess { vaccinations ->
                    _uiState.update { it.copy(vaccinations = vaccinations, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Soft-deletes the record with the given [recordId] and reloads the list.
     */
    fun onDeleteClick(recordId: Long) {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { deleteVaccinationUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-vaccination screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditVaccination(patientId))

    /**
     * Navigates to the edit screen for the vaccination with the given [vaccinationId].
     */
    fun onEditClick(vaccinationId: Long) = navigateTo(Route.AddEditVaccination(patientId, vaccinationId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the vaccination list.
 *
 * @param vaccinations The currently loaded vaccinations.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class VaccinationListUiState(
    val vaccinations: List<Vaccination> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
