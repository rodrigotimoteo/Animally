package com.github.rodrigotimoteo.animally.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class AnimallyNavigationViewModelTest {
    private val navigator = AnimallyNavigator()

    private fun createViewModel() = TestNavigationViewModel(navigator)

    @Test
    fun `navigateTo appends the route to the back stack`() {
        val vm = createViewModel()

        vm.navigateTo(Route.Search)

        assertEquals(2, navigator.backStack.size)
        assertEquals(Route.Search, navigator.backStack.last())
    }

    @Test
    fun `navigateTo preserves destination order`() {
        val vm = createViewModel()

        vm.navigateTo(Route.Search)
        vm.navigateTo(Route.OwnerList)

        assertEquals(Route.Search, navigator.backStack[1])
        assertEquals(Route.OwnerList, navigator.backStack.last())
    }

    @Test
    fun `navigateTo patient detail carries the patient id`() {
        val vm = createViewModel()

        vm.navigateTo(Route.PatientDetail(42L))

        assertEquals(Route.PatientDetail(42L), navigator.backStack.last())
    }

    @Test
    fun `popBackStack removes the last destination`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Settings)

        vm.popBackStack()

        assertEquals(Route.PatientList, navigator.backStack.last())
    }

    @Test
    fun `popBackStack on the initial route keeps PatientList`() {
        val vm = createViewModel()

        vm.popBackStack()

        assertEquals(1, navigator.backStack.size)
        assertEquals(Route.PatientList, navigator.backStack.last())
        assertEquals(Route.PatientList, navigator.currentRoute)
    }

    @Test
    fun `navigateReplace swaps top without growing stack`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Search)

        vm.navigateReplace(Route.Settings)

        assertEquals(2, navigator.backStack.size)
        assertEquals(Route.Settings, navigator.backStack.last())
        assertEquals(Route.PatientList, navigator.backStack.first())
    }

    @Test
    fun `navigateSingleTop avoids duplicate top`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Search)

        vm.navigateSingleTop(Route.Search)

        assertEquals(2, navigator.backStack.size)
        assertEquals(Route.Search, navigator.backStack.last())
    }

    @Test
    fun `navigateSingleTop adds when not duplicate`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Search)

        vm.navigateSingleTop(Route.Settings)

        assertEquals(3, navigator.backStack.size)
        assertEquals(Route.Settings, navigator.backStack.last())
    }

    @Test
    fun `clearAndNavigate leaves single entry`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Search)
        vm.navigateTo(Route.Settings)

        vm.clearAndNavigate(Route.OwnerList)

        assertEquals(1, navigator.backStack.size)
        assertEquals(Route.OwnerList, navigator.backStack.last())
        assertEquals(Route.OwnerList, navigator.currentRoute)
    }

    @Test
    fun `currentRoute reflects the top of the stack`() {
        val vm = createViewModel()

        vm.navigateTo(Route.Settings)

        assertEquals(Route.Settings, navigator.currentRoute)
    }
}

/**
 * Minimal concrete subclass that exercises the navigation base behavior.
 */
private class TestNavigationViewModel(
    navigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(navigator)
