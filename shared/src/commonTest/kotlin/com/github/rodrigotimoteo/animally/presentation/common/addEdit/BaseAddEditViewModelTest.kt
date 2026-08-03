package com.github.rodrigotimoteo.animally.presentation.common.addEdit

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaseAddEditViewModelTest {
    private val navigator = AnimallyNavigator()

    private fun createViewModel() = TestAddEditViewModel(navigator)

    @Test
    fun `formState starts null before any form is loaded`() {
        val vm = createViewModel()

        assertNull(vm.formState.value)
    }

    @Test
    fun `updateForm exposes the loaded form through formState`() {
        val vm = createViewModel()

        vm.loadForm("draft")

        assertEquals("draft", vm.formState.value)
    }

    @Test
    fun `onBack pops the current destination`() {
        val vm = createViewModel()
        vm.navigateTo(Route.Search)
        assertEquals(Route.Search, navigator.backStack.last())

        vm.onBack()

        assertEquals(Route.PatientList, navigator.backStack.last())
    }

    @Test
    fun `onDismissError is a safe no-op when no error is exposed`() {
        val vm = createViewModel()
        vm.loadForm("draft")

        vm.onDismissError()

        assertEquals("draft", vm.formState.value)
    }

    @Test
    fun `save delegates to the concrete subclass implementation`() {
        val vm = createViewModel()

        vm.save()

        assertTrue(vm.saveInvoked)
    }
}

/**
 * Minimal concrete subclass that exercises the abstract base behavior.
 */
private class TestAddEditViewModel(
    navigator: AnimallyNavigator,
) : BaseAddEditViewModel<String>(navigator) {
    var saveInvoked = false

    override fun save() {
        saveInvoked = true
    }

    fun loadForm(value: String) {
        updateForm(value)
    }
}
