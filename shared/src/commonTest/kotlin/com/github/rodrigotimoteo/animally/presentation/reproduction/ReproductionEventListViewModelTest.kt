package com.github.rodrigotimoteo.animally.presentation.reproduction

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.GetReproductionEventsByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
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
class ReproductionEventListViewModelTest {
    private val reproductionRepositoryMock: IReproductionRepository = mock()

    private val getReproductionEventsByPatientUseCase = GetReproductionEventsByPatientUseCase(reproductionRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val reproductionEvent =
        ReproductionEvent(
            id = 1L,
            patientId = 1L,
            eventType = "Breeding",
            date = LocalDate(2026, 1, 15),
            details = "Second cover",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ReproductionEventListViewModel(
            patientId = 1L,
            getReproductionEventsByPatientUseCase = getReproductionEventsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads reproduction events on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val events = listOf(reproductionEvent)
            every { reproductionRepositoryMock.getByPatient(1L) } returns events
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(events, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add reproduction event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproductionRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditReproductionEvent(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit reproduction event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproductionRepositoryMock.getByPatient(1L) } returns listOf(reproductionEvent)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(reproductionEvent.id)

            assertEquals(Route.AddEditReproductionEvent(1L, 1L), navigator.backStack.last())
        }
}
