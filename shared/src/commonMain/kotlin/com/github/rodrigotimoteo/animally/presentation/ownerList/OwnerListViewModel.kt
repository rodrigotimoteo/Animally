package com.github.rodrigotimoteo.animally.presentation.ownerList

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.DeleteOwnerUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.OwnerHasPatientsException
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
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named

/**
 * View model for the owner list screen.
 *
 * @param getOwnerListUseCase Use case for loading the owner list.
 * @param deleteOwnerUseCase Use case for soft-deleting owners.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
@KoinViewModel
class OwnerListViewModel(
    private val getOwnerListUseCase: GetOwnerListUseCase,
    private val deleteOwnerUseCase: DeleteOwnerUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(OwnerListUiState())
    val uiState: StateFlow<OwnerListUiState> = _uiState.asStateFlow()

    init {
        loadOwners()
    }

    /**
     * Reloads the owner list from the repository.
     */
    fun loadOwners() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getOwnerListUseCase() } }
                .onSuccess { owners ->
                    _uiState.update { it.copy(owners = owners, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /**
     * Navigates to the detail screen for the owner with the given [ownerId].
     */
    fun onOwnerClick(ownerId: Long) = navigateTo(Route.OwnerDetail(ownerId))

    /**
     * Navigates to the add-owner screen.
     */
    fun onAddClick() = navigateTo(Route.AddEditOwner())

    /**
     * Soft-deletes the owner with the given [ownerId].
     *
     * Deletion is blocked while the owner still has active patients.
     */
    fun onDeleteClick(ownerId: Long) {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { deleteOwnerUseCase(ownerId) } }
                .onSuccess { loadOwners() }
                .onFailure { error ->
                    val message = if (error is OwnerHasPatientsException) error.message else "Failed to delete owner"
                    _uiState.update { it.copy(errorMessage = message) }
                }
        }
    }

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the owner list screen.
 *
 * @param owners The currently loaded owners.
 * @param isLoading Whether the list is being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class OwnerListUiState(
    val owners: List<Owner> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
