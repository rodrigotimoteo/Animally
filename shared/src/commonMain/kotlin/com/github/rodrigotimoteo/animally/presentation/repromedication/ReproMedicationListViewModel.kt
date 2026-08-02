package com.github.rodrigotimoteo.animally.presentation.repromedication

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.GetReproMedicationsByPatientUseCase
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
 * View model for the reproduction-medication list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose reproduction medications are listed.
 * @param getReproMedicationsByPatientUseCase Use case for loading the reproduction medications.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ReproMedicationListViewModel(
    private val patientId: Long,
    private val getReproMedicationsByPatientUseCase: GetReproMedicationsByPatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(ReproMedicationListUiState())
    val uiState: StateFlow<ReproMedicationListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the reproduction-medication list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getReproMedicationsByPatientUseCase(patientId) } }
                .onSuccess { medications ->
                    _uiState.update { it.copy(records = medications, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-reproduction-medication screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditReproMed(patientId))

    /**
     * Navigates to the edit screen for the reproduction medication with the given [recordId].
     */
    fun onEditClick(recordId: Long) = navigateTo(Route.AddEditReproMed(patientId, recordId))

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the reproduction-medication list.
 *
 * @param records The currently loaded reproduction medications.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ReproMedicationListUiState(
    val records: List<ReproMedication> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
