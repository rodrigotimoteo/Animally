package com.github.rodrigotimoteo.animally.presentation.coggins

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetCogginsStatusUseCase
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CogginsViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private val getCogginsStatusUseCase = GetCogginsStatusUseCase(patientRepositoryMock)

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun patient(cogginsExpiryDate: LocalDate?) =
        Patient(
            id = 1L,
            name = "Charlie",
            cogginsExpiryDate = cogginsExpiryDate,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private fun createViewModel(dispatcher: TestDispatcher) =
        CogginsViewModel(
            getCogginsStatusUseCase = getCogginsStatusUseCase,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `init loads empty alerts when no patient has coggins expiry`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns listOf(patient(null))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.alerts.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

    @Test
    fun `init excludes patient whose coggins is valid beyond lead window`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns
                listOf(patient(today.plus(DatePeriod(days = 365))))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.alerts.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

    @Test
    fun `init failure surfaces error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } throws
                RuntimeException("database down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("database down", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(
                vm.uiState.value.alerts
                    .isEmpty(),
            )
        }

    @Test
    fun `onDismissError clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } throws
                RuntimeException("database down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            assertEquals("database down", vm.uiState.value.errorMessage)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `load recovers after a failed load`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } throws
                RuntimeException("database down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            assertEquals("database down", vm.uiState.value.errorMessage)

            vm.onDismissError()
            assertNull(vm.uiState.value.errorMessage)

            resetAnswers(patientRepositoryMock)
            every { patientRepositoryMock.getPatientList() } returns listOf(patient(null))
            vm.load()
            advanceUntilIdle()
            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertTrue(state.alerts.isEmpty())
        }
}
