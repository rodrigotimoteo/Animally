package com.github.rodrigotimoteo.animally.presentation.labresult

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.DeleteLabResultUseCase
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.GetLabResultsByPatientUseCase
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
 * View model for the lab result list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose lab results are listed.
 * @param getLabResultsByPatientUseCase Use case for loading the lab results.
 * @param deleteLabResultUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class LabResultListViewModel(
    private val patientId: Long,
    private val getLabResultsByPatientUseCase: GetLabResultsByPatientUseCase,
    private val deleteLabResultUseCase: DeleteLabResultUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(LabResultListUiState())
    val uiState: StateFlow<LabResultListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the lab result list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getLabResultsByPatientUseCase(patientId) } }
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
            runCatching { withContext(ioDispatcher) { deleteLabResultUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-lab-result screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditLabResult(patientId))

    /**
     * Navigates to the edit screen for the lab result with the given [labResultId].
     */
    fun onEditClick(labResultId: Long) = navigateTo(Route.AddEditLabResult(patientId, labResultId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the lab result list.
 *
 * @param records The currently loaded lab results.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class LabResultListUiState(
    val records: List<LabResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
