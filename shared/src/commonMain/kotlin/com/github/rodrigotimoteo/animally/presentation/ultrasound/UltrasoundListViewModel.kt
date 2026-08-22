package com.github.rodrigotimoteo.animally.presentation.ultrasound

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.DeleteUltrasoundUseCase
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.GetUltrasoundsByPatientUseCase
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
 * View model for the ultrasound list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose ultrasounds are listed.
 * @param getUltrasoundsByPatientUseCase Use case for loading the ultrasounds.
 * @param deleteUltrasoundUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class UltrasoundListViewModel(
    private val patientId: Long,
    private val getUltrasoundsByPatientUseCase: GetUltrasoundsByPatientUseCase,
    private val deleteUltrasoundUseCase: DeleteUltrasoundUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(UltrasoundListUiState())
    val uiState: StateFlow<UltrasoundListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the ultrasound list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getUltrasoundsByPatientUseCase(patientId) } }
                .onSuccess { ultrasounds ->
                    _uiState.update { it.copy(records = ultrasounds, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteUltrasoundUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-ultrasound screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditUltrasound(patientId))

    /**
     * Navigates to the edit screen for the ultrasound with the given [recordId].
     */
    fun onEditClick(recordId: Long) = navigateTo(Route.AddEditUltrasound(patientId, recordId))

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the ultrasound list.
 *
 * @param records The currently loaded ultrasounds.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class UltrasoundListUiState(
    val records: List<Ultrasound> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
