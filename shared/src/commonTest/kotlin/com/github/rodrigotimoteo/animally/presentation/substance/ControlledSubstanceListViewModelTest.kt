package com.github.rodrigotimoteo.animally.presentation.substance

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.substance.usecase.DeleteControlledSubstanceUseCase
import com.github.rodrigotimoteo.animally.domain.substance.usecase.GetControlledSubstancesByPatientUseCase
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
class ControlledSubstanceListViewModelTest {
    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private val getControlledSubstancesByPatientUseCase = GetControlledSubstancesByPatientUseCase(substanceRepositoryMock)

    private val deleteControlledSubstanceUseCase = DeleteControlledSubstanceUseCase(substanceRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val substance =
        ControlledSubstance(
            id = 1L,
            patientId = 1L,
            drugName = "Xylazine",
            dose = "50",
            unit = "mg",
            date = LocalDate(2026, 1, 15),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ControlledSubstanceListViewModel(
            patientId = 1L,
            getControlledSubstancesByPatientUseCase = getControlledSubstancesByPatientUseCase,
            deleteControlledSubstanceUseCase = deleteControlledSubstanceUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads controlled substances on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(substance)
            every { substanceRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add controlled substance`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { substanceRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditControlledSubstance(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit controlled substance`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { substanceRepositoryMock.getByPatient(1L) } returns listOf(substance)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(substance.id)

            assertEquals(Route.AddEditControlledSubstance(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { substanceRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
