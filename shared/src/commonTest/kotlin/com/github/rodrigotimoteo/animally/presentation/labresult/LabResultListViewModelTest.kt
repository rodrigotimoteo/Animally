package com.github.rodrigotimoteo.animally.presentation.labresult

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.GetLabResultsByPatientUseCase
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
class LabResultListViewModelTest {
    private val labResultRepositoryMock: ILabResultRepository = mock()

    private val getLabResultsByPatientUseCase = GetLabResultsByPatientUseCase(labResultRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val labResult =
        LabResult(
            id = 1L,
            patientId = 1L,
            testType = "CBC",
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        LabResultListViewModel(
            patientId = 1L,
            getLabResultsByPatientUseCase = getLabResultsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads lab result records on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(labResult)
            every { labResultRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add lab result`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { labResultRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditLabResult(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit lab result`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { labResultRepositoryMock.getByPatient(1L) } returns listOf(labResult)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(labResult.id)

            assertEquals(Route.AddEditLabResult(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { labResultRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { labResultRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
