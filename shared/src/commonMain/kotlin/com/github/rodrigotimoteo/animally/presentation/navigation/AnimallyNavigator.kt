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
     * Guard: never leaves the stack empty; if only [Route.PatientList] remains, this is a no-op.
     */
    fun popBackStack() {
        if (backStack.size <= 1) return
        backStack.removeLastOrNull()
    }

    /**
     * Replace the current top of the stack with [route].
     * If the stack is empty, behaves like [navigateTo].
     */
    fun navigateReplace(route: Route) {
        if (backStack.isEmpty()) {
            backStack.add(route)
        } else {
            backStack[backStack.lastIndex] = route
        }
    }

    /**
     * Navigate to [route] only if it is not already on top of the stack.
     * Avoids duplicate entries when the user triggers the same navigation repeatedly.
     */
    fun navigateSingleTop(route: Route) {
        if (backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    /**
     * Clear the entire back stack and navigate to [route], leaving it as the sole entry.
     */
    fun clearAndNavigate(route: Route) {
        backStack.clear()
        backStack.add(route)
    }
}
