package com.github.rodrigotimoteo.animally.presentation.imaging

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.DeleteImagingUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingListByPatientUseCase
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
 * View model for the imaging list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose imaging records are listed.
 * @param getImagingListByPatientUseCase Use case for loading the imaging records.
 * @param deleteImagingUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ImagingListViewModel(
    private val patientId: Long,
    private val getImagingListByPatientUseCase: GetImagingListByPatientUseCase,
    private val deleteImagingUseCase: DeleteImagingUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(ImagingListUiState())
    val uiState: StateFlow<ImagingListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the imaging list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getImagingListByPatientUseCase(patientId) } }
                .onSuccess { records ->
                    _uiState.update { it.copy(records = records, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteImagingUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-imaging screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditImaging(patientId))

    /**
     * Navigates to the edit screen for the imaging record with the given [imagingId].
     */
    fun onEditClick(imagingId: Long) = navigateTo(Route.AddEditImaging(patientId, imagingId))

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
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the imaging list.
 *
 * @param records The currently loaded imaging records.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ImagingListUiState(
    val records: List<Imaging> = emptyList(),
    val displayState: ListDisplayState = ListDisplayState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Imaging records matching the current search query. */
    val filteredRecords: List<Imaging>
        get() =
            records.filterBySearch(displayState.searchQuery) { record ->
                listOfNotNull(
                    record.type,
                    record.date.toString(),
                    record.findings,
                    record.imageUris,
                    record.vetName,
                    record.notes,
                ).joinToString(" ")
            }

    /** Imaging records shown after applying search and the collapsed-list limit. */
    val visibleRecords: List<Imaging>
        get() = filteredRecords.visibleForListDisplay(displayState)
}
