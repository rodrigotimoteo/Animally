package com.github.rodrigotimoteo.animally.presentation.ownerDetail

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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
 * View model for the owner detail screen.
 *
 * @param ownerId The id of the owner to display.
 * @param getOwnerDetailUseCase Use case for loading the owner.
 * @param patientRepository Repository for loading linked patients.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class OwnerDetailViewModel(
    private val ownerId: Long,
    private val getOwnerDetailUseCase: GetOwnerDetailUseCase,
    private val patientRepository: IPatientRepository,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(OwnerDetailUiState())
    val uiState: StateFlow<OwnerDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the owner and its linked patients.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    getOwnerDetailUseCase(ownerId) to patientRepository.getPatientsByOwnerId(ownerId)
                }
            }.onSuccess { (owner, patients) ->
                _uiState.update {
                    it.copy(owner = owner, patients = patients, isLoading = false)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Navigates back to the previous destination.
     */
    fun onBack() = popBackStack()

    /**
     * Navigates to the edit screen for the current owner.
     */
    fun onEditClick() {
        _uiState.value.owner?.let { navigateTo(Route.AddEditOwner(it.id)) }
    }

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the owner detail screen.
 *
 * @param owner The loaded owner, or `null` when not found.
 * @param patients The active patients linked to the owner.
 * @param isLoading Whether the data is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class OwnerDetailUiState(
    val owner: Owner? = null,
    val patients: List<Patient> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
