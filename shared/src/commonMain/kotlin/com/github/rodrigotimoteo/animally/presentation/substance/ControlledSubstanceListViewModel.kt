package com.github.rodrigotimoteo.animally.presentation.substance

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.substance.usecase.DeleteControlledSubstanceUseCase
import com.github.rodrigotimoteo.animally.domain.substance.usecase.GetControlledSubstancesByPatientUseCase
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
 * View model for the controlled-substance list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose controlled-substance records are listed.
 * @param getControlledSubstancesByPatientUseCase Use case for loading the controlled-substance records.
 * @param deleteControlledSubstanceUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ControlledSubstanceListViewModel(
    private val patientId: Long,
    private val getControlledSubstancesByPatientUseCase: GetControlledSubstancesByPatientUseCase,
    private val deleteControlledSubstanceUseCase: DeleteControlledSubstanceUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(ControlledSubstanceListUiState())
    val uiState: StateFlow<ControlledSubstanceListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the controlled-substance list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getControlledSubstancesByPatientUseCase(patientId) } }
                .onSuccess { substances ->
                    _uiState.update { it.copy(records = substances, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteControlledSubstanceUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-controlled-substance screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditControlledSubstance(patientId))

    /**
     * Navigates to the edit screen for the controlled-substance record with the given [substanceId].
     */
    fun onEditClick(substanceId: Long) = navigateTo(Route.AddEditControlledSubstance(patientId, substanceId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the controlled-substance list.
 *
 * @param records The currently loaded controlled-substance records.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ControlledSubstanceListUiState(
    val records: List<ControlledSubstance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
