package com.github.rodrigotimoteo.animally.presentation.weight

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.GetWeightsByPatientUseCase
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
 * View model for the weight list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose weight entries are listed.
 * @param getWeightsByPatientUseCase Use case for loading the weight entries.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class WeightListViewModel(
    private val patientId: Long,
    private val getWeightsByPatientUseCase: GetWeightsByPatientUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(WeightListUiState())
    val uiState: StateFlow<WeightListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the weight list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getWeightsByPatientUseCase(patientId) } }
                .onSuccess { records ->
                    _uiState.update { it.copy(records = records, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-weight screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditWeight(patientId))

    /**
     * Navigates to the edit screen for the weight entry with the given [weightId].
     */
    fun onEditClick(weightId: Long) = navigateTo(Route.AddEditWeight(patientId, weightId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the weight list.
 *
 * @param records The currently loaded weight entries.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class WeightListUiState(
    val records: List<Weight> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
