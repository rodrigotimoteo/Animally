package com.github.rodrigotimoteo.animally.presentation.common.addEdit

import app.cash.turbine.test
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseAddEditViewModelTest {
    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    @Test
    fun `successful save emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel()
            vm.saveSucceeds = true

            vm.effects.test {
                vm.save()

                assertEquals(EditEffect.Saved, awaitItem())
            }
        }

    @Test
    fun `failed save emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel()
            vm.saveSucceeds = false

            vm.effects.test {
                vm.save()

                expectNoEvents()
            }
        }
}

/**
 * Minimal concrete subclass that exercises the abstract base behavior.
 */
private class TestAddEditViewModel(
    navigator: AnimallyNavigator,
) : BaseAddEditViewModel<String>(navigator) {
    var saveInvoked = false
    var saveSucceeds = true

    override fun save() {
        saveInvoked = true
        if (saveSucceeds) {
            emitSaved()
        }
    }

    fun loadForm(value: String) {
        updateForm(value)
    }
}
