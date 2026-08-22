package com.github.rodrigotimoteo.animally.presentation.gestation

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.DeleteGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GetGestationsByPatientUseCase
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
 * View model for the gestation list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose gestation records are listed.
 * @param getGestationsByPatientUseCase Use case for loading the gestation records.
 * @param deleteGestationUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class GestationListViewModel(
    private val patientId: Long,
    private val getGestationsByPatientUseCase: GetGestationsByPatientUseCase,
    private val deleteGestationUseCase: DeleteGestationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(GestationListUiState())
    val uiState: StateFlow<GestationListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the gestation list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getGestationsByPatientUseCase(patientId) } }
                .onSuccess { gestations ->
                    _uiState.update { it.copy(records = gestations, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteGestationUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-gestation screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditGestation(patientId))

    /**
     * Navigates to the edit screen for the gestation record with the given [recordId].
     */
    fun onEditClick(recordId: Long) = navigateTo(Route.AddEditGestation(patientId, recordId))

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the gestation list.
 *
 * @param records The currently loaded gestation records.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class GestationListUiState(
    val records: List<Gestation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
