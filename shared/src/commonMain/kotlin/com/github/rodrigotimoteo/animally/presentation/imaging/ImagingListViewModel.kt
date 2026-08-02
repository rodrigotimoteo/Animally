package com.github.rodrigotimoteo.animally.presentation.imaging

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingListByPatientUseCase
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
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ImagingListViewModel(
    private val patientId: Long,
    private val getImagingListByPatientUseCase: GetImagingListByPatientUseCase,
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
     * Navigates to the add-imaging screen for the current patient.
     */
    fun onAddClick() = navigateTo(Route.AddEditImaging(patientId))

    /**
     * Navigates to the edit screen for the imaging record with the given [imagingId].
     */
    fun onEditClick(imagingId: Long) = navigateTo(Route.AddEditImaging(patientId, imagingId))

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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
