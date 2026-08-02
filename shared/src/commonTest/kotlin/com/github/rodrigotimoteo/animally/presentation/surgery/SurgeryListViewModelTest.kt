package com.github.rodrigotimoteo.animally.presentation.surgery

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.GetSurgeriesByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SurgeryListViewModelTest {
    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private val getSurgeriesByPatientUseCase = GetSurgeriesByPatientUseCase(surgeryRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val surgery =
        Surgery(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            type = "Colic surgery",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        SurgeryListViewModel(
            patientId = 1L,
            getSurgeriesByPatientUseCase = getSurgeriesByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads surgeries on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(surgery)
            every { surgeryRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add surgery`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { surgeryRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditSurgery(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit surgery`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { surgeryRepositoryMock.getByPatient(1L) } returns listOf(surgery)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(surgery.id)

            assertEquals(Route.AddEditSurgery(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { surgeryRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
