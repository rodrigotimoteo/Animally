package com.github.rodrigotimoteo.animally.presentation.dentistry

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.DeleteDentistryUseCase
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.GetDentistryListByPatientUseCase
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
 * View model for the dentistry list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose dentistry records are listed.
 * @param getDentistryListByPatientUseCase Use case for loading the dentistry records.
 * @param deleteDentistryUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class DentistryListViewModel(
    private val patientId: Long,
    private val getDentistryListByPatientUseCase: GetDentistryListByPatientUseCase,
    private val deleteDentistryUseCase: DeleteDentistryUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(DentistryListUiState())
    val uiState: StateFlow<DentistryListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the dentistry list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getDentistryListByPatientUseCase(patientId) } }
                .onSuccess { dentistryRecords ->
                    _uiState.update { it.copy(records = dentistryRecords, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteDentistryUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-dentistry screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditDentistry(patientId))

    /**
     * Navigates to the edit screen for the dentistry record with the given [dentistryId].
     */
    fun onEditClick(dentistryId: Long) = navigateTo(Route.AddEditDentistry(patientId, dentistryId))

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
 * UI state for the dentistry list.
 *
 * @param records The currently loaded dentistry records.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class DentistryListUiState(
    val records: List<Dentistry> = emptyList(),
    val displayState: ListDisplayState = ListDisplayState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Dentistry records matching the current search query. */
    val filteredRecords: List<Dentistry>
        get() =
            records.filterBySearch(displayState.searchQuery) { record ->
                listOfNotNull(
                    record.date.toString(),
                    record.findings,
                    record.treatment,
                    record.nextDueDate?.toString(),
                    record.vetName,
                    record.notes,
                ).joinToString(" ")
            }

    /** Dentistry records shown after applying search and the collapsed-list limit. */
    val visibleRecords: List<Dentistry>
        get() = filteredRecords.visibleForListDisplay(displayState)
}
