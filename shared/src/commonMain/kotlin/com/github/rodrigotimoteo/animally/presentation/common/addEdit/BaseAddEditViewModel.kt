package com.github.rodrigotimoteo.animally.presentation.common.addEdit

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A one-shot signal emitted by an add/edit screen after a successful save.
 */
sealed interface EditEffect {
    /** Emitted when the current form has been persisted successfully. */
    data object Saved : EditEffect
}

/**
 * Base class for add/edit screens that drive a single [F] form state.
 *
 * @param animallyNavigator The navigator to use for navigation.
 */
abstract class BaseAddEditViewModel<F>(
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _formState = MutableStateFlow<F?>(null)
    private val _effects = MutableSharedFlow<EditEffect>(replay = 0, extraBufferCapacity = 1)

    /** The current form state, or `null` until the form has been loaded. */
    val formState: StateFlow<F?> = _formState.asStateFlow()

    /**
     * One-shot effects emitted by this screen. Consumers collect this flow and react
     * (for example, navigating back when [EditEffect.Saved] is emitted).
     */
    val effects: SharedFlow<EditEffect> = _effects.asSharedFlow()

    /**
     * Emits [EditEffect.Saved] after the current form has been persisted successfully.
     */
    protected fun emitSaved() {
        _effects.tryEmit(EditEffect.Saved)
    }

    /**
     * Updates the current form state.
     *
     * @param form the new form state.
     */
    protected fun updateForm(form: F) {
        _formState.value = form
    }

    /**
     * Validates and persists the current form state.
     */
    abstract fun save()

    /**
     * Navigates back to the previous destination.
     */
    fun onBack() = popBackStack()

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        // no-op by default; overridden when the form exposes errors
    }
}
