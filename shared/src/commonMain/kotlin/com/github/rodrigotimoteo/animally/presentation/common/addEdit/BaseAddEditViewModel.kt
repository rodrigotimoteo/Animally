package com.github.rodrigotimoteo.animally.presentation.common.addEdit

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base class for add/edit screens that drive a single [F] form state.
 *
 * @param animallyNavigator The navigator to use for navigation.
 */
abstract class BaseAddEditViewModel<F>(
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _formState = MutableStateFlow<F?>(null)

    /** The current form state, or `null` until the form has been loaded. */
    val formState: StateFlow<F?> = _formState.asStateFlow()

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
