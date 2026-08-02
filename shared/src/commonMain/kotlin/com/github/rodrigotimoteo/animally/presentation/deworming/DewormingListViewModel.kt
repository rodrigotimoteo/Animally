package com.github.rodrigotimoteo.animally.presentation.deworming

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.GetDewormingsByPatientUseCase
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
 * View model for the deworming list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose dewormings are listed.
 * @param getDewormingsByPatientUseCase Use case for loading the dewormings.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class DewormingListViewModel(
    private val patientId: Long,
    private val getDewormingsByPatientUseCase: GetDewormingsByPatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(DewormingListUiState())
    val uiState: StateFlow<DewormingListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the deworming list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getDewormingsByPatientUseCase(patientId) } }
                .onSuccess { dewormings ->
                    _uiState.update { it.copy(records = dewormings, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-deworming screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditDeworming(patientId))

    /**
     * Navigates to the edit screen for the deworming with the given [dewormingId].
     */
    fun onEditClick(dewormingId: Long) = navigateTo(Route.AddEditDeworming(patientId, dewormingId))

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the deworming list.
 *
 * @param records The currently loaded dewormings.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class DewormingListUiState(
    val records: List<Deworming> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
