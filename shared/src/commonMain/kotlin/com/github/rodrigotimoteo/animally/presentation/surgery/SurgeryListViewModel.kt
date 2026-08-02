package com.github.rodrigotimoteo.animally.presentation.surgery

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.GetSurgeriesByPatientUseCase
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
 * View model for the surgery list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose surgeries are listed.
 * @param getSurgeriesByPatientUseCase Use case for loading the surgeries.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class SurgeryListViewModel(
    private val patientId: Long,
    private val getSurgeriesByPatientUseCase: GetSurgeriesByPatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(SurgeryListUiState())
    val uiState: StateFlow<SurgeryListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the surgery list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getSurgeriesByPatientUseCase(patientId) } }
                .onSuccess { surgeries ->
                    _uiState.update { it.copy(records = surgeries, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-surgery screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditSurgery(patientId))

    /**
     * Navigates to the edit screen for the surgery with the given [surgeryId].
     */
    fun onEditClick(surgeryId: Long) = navigateTo(Route.AddEditSurgery(patientId, surgeryId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the surgery list.
 *
 * @param records The currently loaded surgeries.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class SurgeryListUiState(
    val records: List<Surgery> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
