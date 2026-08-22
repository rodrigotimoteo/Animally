package com.github.rodrigotimoteo.animally.presentation.imaging

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.DeleteImagingUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingListByPatientUseCase
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
class ImagingListViewModelTest {
    private val imagingRepositoryMock: IImagingRepository = mock()

    private val getImagingListByPatientUseCase = GetImagingListByPatientUseCase(imagingRepositoryMock)

    private val deleteImagingUseCase = DeleteImagingUseCase(imagingRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val imaging =
        Imaging(
            id = 1L,
            patientId = 1L,
            type = "X-ray",
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ImagingListViewModel(
            patientId = 1L,
            getImagingListByPatientUseCase = getImagingListByPatientUseCase,
            deleteImagingUseCase = deleteImagingUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads imaging records on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(imaging)
            every { imagingRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add imaging`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditImaging(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit imaging`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.getByPatient(1L) } returns listOf(imaging)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(imaging.id)

            assertEquals(Route.AddEditImaging(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
