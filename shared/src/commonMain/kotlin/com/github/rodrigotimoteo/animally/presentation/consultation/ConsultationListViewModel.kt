package com.github.rodrigotimoteo.animally.presentation.consultation

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.DeleteConsultationUseCase
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.GetConsultationsByPatientUseCase
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
 * View model for the consultation list embedded in the patient detail screen.
 *
 * @param patientId The id of the patient whose consultations are listed.
 * @param getConsultationsByPatientUseCase Use case for loading the consultations.
 * @param deleteConsultationUseCase Use case for soft-deleting a record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ConsultationListViewModel(
    private val patientId: Long,
    private val getConsultationsByPatientUseCase: GetConsultationsByPatientUseCase,
    private val deleteConsultationUseCase: DeleteConsultationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(ConsultationListUiState())
    val uiState: StateFlow<ConsultationListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the consultation list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getConsultationsByPatientUseCase(patientId) } }
                .onSuccess { consultations ->
                    _uiState.update { it.copy(consultations = consultations, isLoading = false) }
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
            runCatching { withContext(ioDispatcher) { deleteConsultationUseCase(recordId) } }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the add-consultation screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditConsultation(patientId))

    /**
     * Navigates to the edit screen for the consultation with the given [consultationId].
     */
    fun onEditClick(consultationId: Long) = navigateTo(Route.AddEditConsultation(patientId, consultationId))

    /**
     * Dismisses the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the consultation list.
 *
 * @param consultations The currently loaded consultations.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class ConsultationListUiState(
    val consultations: List<Consultation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
