package com.github.rodrigotimoteo.animally.presentation.farrier

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.DeleteFarrierVisitUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.GetFarrierVisitsByPatientUseCase
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
 * View model for the farrier visit list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose farrier visits are listed.
 * @param getFarrierVisitsByPatientUseCase Use case for loading the farrier visits.
 * @param deleteFarrierVisitUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class FarrierVisitListViewModel(
    private val patientId: Long,
    private val getFarrierVisitsByPatientUseCase: GetFarrierVisitsByPatientUseCase,
    private val deleteFarrierVisitUseCase: DeleteFarrierVisitUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(FarrierVisitListUiState())
    val uiState: StateFlow<FarrierVisitListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the farrier visit list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getFarrierVisitsByPatientUseCase(patientId) } }
                .onSuccess { visits ->
                    _uiState.update { it.copy(records = visits, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteFarrierVisitUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-farrier-visit screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditFarrierVisit(patientId))

    /**
     * Navigates to the edit screen for the farrier visit with the given [farrierVisitId].
     */
    fun onEditClick(farrierVisitId: Long) = navigateTo(Route.AddEditFarrierVisit(patientId, farrierVisitId))

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the farrier visit list.
 *
 * @param records The currently loaded farrier visits.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class FarrierVisitListUiState(
    val records: List<FarrierVisit> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
