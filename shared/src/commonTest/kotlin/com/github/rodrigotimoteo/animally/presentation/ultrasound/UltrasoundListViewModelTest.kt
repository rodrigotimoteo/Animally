package com.github.rodrigotimoteo.animally.presentation.ultrasound

import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.DeleteUltrasoundUseCase
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.GetUltrasoundsByPatientUseCase
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
class UltrasoundListViewModelTest {
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    private val getUltrasoundsByPatientUseCase = GetUltrasoundsByPatientUseCase(ultrasoundRepositoryMock)

    private val deleteUltrasoundUseCase = DeleteUltrasoundUseCase(ultrasoundRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val ultrasound =
        Ultrasound(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            ovaryStatus = "Active",
            follicleSizeMm = 32.0,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        UltrasoundListViewModel(
            patientId = 1L,
            getUltrasoundsByPatientUseCase = getUltrasoundsByPatientUseCase,
            deleteUltrasoundUseCase = deleteUltrasoundUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads ultrasounds on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val ultrasounds = listOf(ultrasound)
            every { ultrasoundRepositoryMock.getByPatient(1L) } returns ultrasounds
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(ultrasounds, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add ultrasound`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ultrasoundRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditUltrasound(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit ultrasound`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ultrasoundRepositoryMock.getByPatient(1L) } returns listOf(ultrasound)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(ultrasound.id)

            assertEquals(Route.AddEditUltrasound(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ultrasoundRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ultrasoundRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
