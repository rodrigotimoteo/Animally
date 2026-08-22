package com.github.rodrigotimoteo.animally.presentation.deworming

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.DeleteDewormingUseCase
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.GetDewormingsByPatientUseCase
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
class DewormingListViewModelTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private val getDewormingsByPatientUseCase = GetDewormingsByPatientUseCase(dewormingRepositoryMock)

    private val deleteDewormingUseCase = DeleteDewormingUseCase(dewormingRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val deworming =
        Deworming(
            id = 1L,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2026, 1, 15),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        DewormingListViewModel(
            patientId = 1L,
            getDewormingsByPatientUseCase = getDewormingsByPatientUseCase,
            deleteDewormingUseCase = deleteDewormingUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads dewormings on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val dewormings = listOf(deworming)
            every { dewormingRepositoryMock.getByPatient(1L) } returns dewormings
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(dewormings, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add deworming`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dewormingRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditDeworming(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit deworming`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dewormingRepositoryMock.getByPatient(1L) } returns listOf(deworming)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(deworming.id)

            assertEquals(Route.AddEditDeworming(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure surfaces error and dismiss clears it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dewormingRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
