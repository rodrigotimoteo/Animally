package com.github.rodrigotimoteo.animally.presentation.lameness

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.GetLamenessListByPatientUseCase
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
 * View model for the lameness list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose lameness evaluations are listed.
 * @param getLamenessListByPatientUseCase Use case for loading the lameness evaluations.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class LamenessListViewModel(
    private val patientId: Long,
    private val getLamenessListByPatientUseCase: GetLamenessListByPatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(LamenessListUiState())
    val uiState: StateFlow<LamenessListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the lameness list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getLamenessListByPatientUseCase(patientId) } }
                .onSuccess { lamenessRecords ->
                    _uiState.update { it.copy(records = lamenessRecords, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-lameness screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditLameness(patientId))

    /**
     * Navigates to the edit screen for the lameness evaluation with the given [lamenessId].
     */
    fun onEditClick(lamenessId: Long) = navigateTo(Route.AddEditLameness(patientId, lamenessId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the lameness list.
 *
 * @param records The currently loaded lameness evaluations.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class LamenessListUiState(
    val records: List<Lameness> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
