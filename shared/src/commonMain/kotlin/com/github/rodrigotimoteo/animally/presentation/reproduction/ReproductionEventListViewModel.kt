package com.github.rodrigotimoteo.animally.presentation.reproduction

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.DeleteReproductionEventUseCase
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.GetReproductionEventsByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayState
import com.github.rodrigotimoteo.animally.presentation.common.list.filterBySearch
import com.github.rodrigotimoteo.animally.presentation.common.list.visibleForListDisplay
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
 * View model for the reproduction-event list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose reproduction events are listed.
 * @param getReproductionEventsByPatientUseCase Use case for loading the reproduction events.
 * @param deleteReproductionEventUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ReproductionEventListViewModel(
    private val patientId: Long,
    private val getReproductionEventsByPatientUseCase: GetReproductionEventsByPatientUseCase,
    private val deleteReproductionEventUseCase: DeleteReproductionEventUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(ReproductionEventListUiState())
    val uiState: StateFlow<ReproductionEventListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the reproduction-event list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getReproductionEventsByPatientUseCase(patientId) } }
                .onSuccess { events ->
                    _uiState.update { it.copy(records = events, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteReproductionEventUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-reproduction-event screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditReproductionEvent(patientId))

    /**
     * Navigates to the edit screen for the reproduction event with the given [recordId].
     */
    fun onEditClick(recordId: Long) = navigateTo(Route.AddEditReproductionEvent(patientId, recordId))

    /** Opens the inline search field for this list. */
    fun onSearchClick() {
        _uiState.update { it.copy(displayState = it.displayState.copy(searchQuery = "")) }
    }

    /** Updates the inline search query. */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(displayState = it.displayState.copy(searchQuery = query)) }
    }

    /** Closes the inline search field and clears its query. */
    fun onCloseSearch() {
        _uiState.update { it.copy(displayState = it.displayState.copy(searchQuery = null)) }
    }

    /** Toggles whether all matching records are shown. */
    fun onToggleExpanded() {
        _uiState.update { it.copy(displayState = it.displayState.copy(isExpanded = !it.displayState.isExpanded)) }
    }

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the reproduction-event list.
 *
 * @param records The currently loaded reproduction events.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ReproductionEventListUiState(
    val records: List<ReproductionEvent> = emptyList(),
    val displayState: ListDisplayState = ListDisplayState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Reproduction events matching the current search query. */
    val filteredRecords: List<ReproductionEvent>
        get() =
            records.filterBySearch(displayState.searchQuery) { record ->
                listOfNotNull(
                    record.eventType,
                    record.date.toString(),
                    record.details,
                    record.initialExamFindings,
                    record.stallionName,
                    record.breedingType,
                    record.vetName,
                    record.notes,
                ).joinToString(" ")
            }

    /** Reproduction events shown after applying search and the collapsed-list limit. */
    val visibleRecords: List<ReproductionEvent>
        get() = filteredRecords.visibleForListDisplay(displayState)
}
