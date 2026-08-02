package com.github.rodrigotimoteo.animally.presentation.patientList

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.DeletePatientUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientListUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private val getPatientListUseCase = GetPatientListUseCase(patientRepositoryMock)

    private val deletePatientUseCase = DeletePatientUseCase(patientRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val patient =
        Patient(
            id = 1L,
            name = "Midnight",
            species = "Equine",
            breed = "Lusitano",
            microchipId = "981000123456789",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) = PatientListViewModel(getPatientListUseCase, deletePatientUseCase, navigator, dispatcher)

    @Test
    fun `loads patients on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val patients = listOf(patient)
            every { patientRepositoryMock.getPatientList() } returns patients
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(patients, vm.uiState.value.patients)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on patient click navigates to patient detail`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns listOf(patient)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onPatientClick(patient.id)

            assertEquals(Route.PatientDetail(patient.id), navigator.backStack.last())
        }

    @Test
    fun `delete success reloads patients`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns listOf(patient)
            every { patientRepositoryMock.countActiveRecords(patient.id) } returns 0L
            every { patientRepositoryMock.setInactive(patient.id, any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onDeleteClick(patient.id)
            advanceUntilIdle()

            verify(VerifyMode.exactly(2)) { patientRepositoryMock.getPatientList() }
            verify(VerifyMode.exactly(1)) { patientRepositoryMock.setInactive(patient.id, any()) }
        }

    @Test
    fun `delete blocked sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns listOf(patient)
            every { patientRepositoryMock.countActiveRecords(patient.id) } returns 2L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onDeleteClick(patient.id)
            advanceUntilIdle()

            assertEquals("Patient has 2 records. Delete records first or use soft delete.", vm.uiState.value.errorMessage)
            verify(VerifyMode.exactly(0)) { patientRepositoryMock.setInactive(patient.id, any()) }

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
