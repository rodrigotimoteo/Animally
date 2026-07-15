package com.github.rodrigotimoteo.animally.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.koin.core.annotation.Single

/**
 *
 * @author rodrigotimoteo
 */
@Single
class AnimallyNavigator {
    /** The current destination in the navigation graph. */
    val backStack: SnapshotStateList<Route> = mutableStateListOf(Route.PatientList)

    /** The current destination in the navigation graph. */
    val currentRoute: Route? get() = backStack.lastOrNull()

    /**
     * Navigate to the given [route].
     *
     * @param route The destination to navigate to.
     */
    fun navigateTo(route: Route) {
        backStack.add(route)
    }

    /**
     * Pop the current destination from the navigation graph.
     */
    fun popBackStack() {
        backStack.removeLastOrNull()
    }
}
