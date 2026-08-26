package com.github.rodrigotimoteo.animally.presentation.medication

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.medication.usecase.DeleteMedicationUseCase
import com.github.rodrigotimoteo.animally.domain.medication.usecase.GetMedicationsByPatientUseCase
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
 * View model for the medication list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose medications are listed.
 * @param getMedicationsByPatientUseCase Use case for loading the medications.
 * @param deleteMedicationUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class MedicationListViewModel(
    private val patientId: Long,
    private val getMedicationsByPatientUseCase: GetMedicationsByPatientUseCase,
    private val deleteMedicationUseCase: DeleteMedicationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(MedicationListUiState())
    val uiState: StateFlow<MedicationListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the medication list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getMedicationsByPatientUseCase(patientId) } }
                .onSuccess { medications ->
                    _uiState.update { it.copy(records = medications, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteMedicationUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-medication screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditMedication(patientId))

    /**
     * Navigates to the edit screen for the medication with the given [medicationId].
     */
    fun onEditClick(medicationId: Long) = navigateTo(Route.AddEditMedication(patientId, medicationId))

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
 * UI state for the medication list.
 *
 * @param records The currently loaded medications.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class MedicationListUiState(
    val records: List<Medication> = emptyList(),
    val displayState: ListDisplayState = ListDisplayState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Medications matching the current search query. */
    val filteredRecords: List<Medication>
        get() =
            records.filterBySearch(displayState.searchQuery) { record ->
                listOfNotNull(
                    record.name,
                    record.dosage,
                    record.route,
                    record.frequency,
                    record.startDate?.toString(),
                    record.endDate?.toString(),
                    record.prescribedBy,
                    record.notes,
                ).joinToString(" ")
            }

    /** Medications shown after applying search and the collapsed-list limit. */
    val visibleRecords: List<Medication>
        get() = filteredRecords.visibleForListDisplay(displayState)
}
