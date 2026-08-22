package com.github.rodrigotimoteo.animally.presentation.weight

import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.DeleteWeightUseCase
import com.github.rodrigotimoteo.animally.domain.weight.usecase.GetWeightsByPatientUseCase
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
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WeightListViewModelTest {
    private val weightRepositoryMock: IWeightRepository = mock()

    private val getWeightsByPatientUseCase = GetWeightsByPatientUseCase(weightRepositoryMock)

    private val deleteWeightUseCase = DeleteWeightUseCase(weightRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val weight =
        Weight(
            id = 1L,
            patientId = 1L,
            weightKg = 520.0,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        WeightListViewModel(
            patientId = 1L,
            getWeightsByPatientUseCase = getWeightsByPatientUseCase,
            deleteWeightUseCase = deleteWeightUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads weight records on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(weight)
            every { weightRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add weight`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditWeight(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit weight`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.getByPatient(1L) } returns listOf(weight)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(weight.id)

            assertEquals(Route.AddEditWeight(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
