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

    /**
     * Replace the current destination with [route].
     */
    fun navigateReplace(route: Route) {
        animallyNavigator.navigateReplace(route)
    }

    /**
     * Navigate to [route] only if it is not already on top.
     */
    fun navigateSingleTop(route: Route) {
        animallyNavigator.navigateSingleTop(route)
    }

    /**
     * Clear the stack and navigate to [route].
     */
    fun clearAndNavigate(route: Route) {
        animallyNavigator.clearAndNavigate(route)
    }
}
