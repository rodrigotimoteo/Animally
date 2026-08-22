package com.github.rodrigotimoteo.animally.presentation.farrier

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.DeleteFarrierVisitUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.GetFarrierVisitsByPatientUseCase
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
class FarrierVisitListViewModelTest {
    private val farrierVisitRepositoryMock: IFarrierVisitRepository = mock()

    private val getFarrierVisitsByPatientUseCase = GetFarrierVisitsByPatientUseCase(farrierVisitRepositoryMock)

    private val deleteFarrierVisitUseCase = DeleteFarrierVisitUseCase(farrierVisitRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val visit =
        FarrierVisit(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            trimOrShoe = "Trim",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        FarrierVisitListViewModel(
            patientId = 1L,
            getFarrierVisitsByPatientUseCase = getFarrierVisitsByPatientUseCase,
            deleteFarrierVisitUseCase = deleteFarrierVisitUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads farrier visits on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val visits = listOf(visit)
            every { farrierVisitRepositoryMock.getByPatient(1L) } returns visits
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(visits, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add farrier visit`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { farrierVisitRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditFarrierVisit(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit farrier visit`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { farrierVisitRepositoryMock.getByPatient(1L) } returns listOf(visit)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(visit.id)

            assertEquals(Route.AddEditFarrierVisit(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure surfaces error and dismiss clears it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { farrierVisitRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
