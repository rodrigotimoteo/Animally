package com.github.rodrigotimoteo.animally.presentation.gestation

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GetGestationsByPatientUseCase
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
class GestationListViewModelTest {
    private val gestationRepositoryMock: IGestationRepository = mock()

    private val getGestationsByPatientUseCase = GetGestationsByPatientUseCase(gestationRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val gestation =
        Gestation(
            id = 1L,
            patientId = 1L,
            breedingDate = LocalDate(2026, 1, 1),
            expectedDueDate = LocalDate(2026, 12, 7),
            gestationDays = 30,
            status = "Active",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        GestationListViewModel(
            patientId = 1L,
            getGestationsByPatientUseCase = getGestationsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads gestations on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gestations = listOf(gestation)
            every { gestationRepositoryMock.getByPatient(1L) } returns gestations
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(gestations, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add gestation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { gestationRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditGestation(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit gestation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { gestationRepositoryMock.getByPatient(1L) } returns listOf(gestation)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(gestation.id)

            assertEquals(Route.AddEditGestation(1L, 1L), navigator.backStack.last())
        }
}
