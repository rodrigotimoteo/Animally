package com.github.rodrigotimoteo.animally.presentation.embryotransfer

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase.DeleteEmbryoTransferUseCase
import com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase.GetEmbryoTransfersByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named

/**
 * View model for the embryo transfer list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose embryo transfers are listed.
 * @param getEmbryoTransfersByPatientUseCase Use case for loading the records.
 * @param deleteEmbryoTransferUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class EmbryoTransferListViewModel(
    private val patientId: Long,
    private val getEmbryoTransfersByPatientUseCase: GetEmbryoTransfersByPatientUseCase,
    private val deleteEmbryoTransferUseCase: DeleteEmbryoTransferUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(EmbryoTransferListUiState())
    val uiState: StateFlow<EmbryoTransferListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the embryo transfer list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getEmbryoTransfersByPatientUseCase(patientId) } }
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
            runCatching { withContext(ioDispatcher) { deleteEmbryoTransferUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the embryo transfer list.
 *
 * @param records The currently loaded records.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class EmbryoTransferListUiState(
    val records: List<EmbryoTransfer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
