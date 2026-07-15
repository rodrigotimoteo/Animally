package com.github.rodrigotimoteo.animally.presentation.navigation

import androidx.lifecycle.ViewModel

/**
 * Base class for all view models that need to navigate
 *
 * @param animallyNavigator The navigator to use for navigation.
 * @author rodrigotimoteo
 */
abstract class AnimallyNavigationViewModel(
    protected val animallyNavigator: AnimallyNavigator,
) : ViewModel() {
    /**
     * Navigate to the given [route].
     *
     * @param route The destination to navigate to.
     */
    fun navigateTo(route: Route) {
        animallyNavigator.navigateTo(route)
    }

    /**
     * Pop the current destination from the navigation graph.
     */
    fun popBackStack() {
        animallyNavigator.popBackStack()
    }
}
