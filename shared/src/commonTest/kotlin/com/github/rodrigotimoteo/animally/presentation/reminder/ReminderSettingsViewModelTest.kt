package com.github.rodrigotimoteo.animally.presentation.reminder

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.notification.NotificationPermissionController
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetDentistryRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetVaccinationRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
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
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSettingsViewModelTest {
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private val permissionControllerMock: NotificationPermissionController = mock()

    private val getVaccinationRemindersUseCase =
        GetVaccinationRemindersUseCase(vaccinationRepositoryMock, patientRepositoryMock)

    private val getDentistryRemindersUseCase =
        GetDentistryRemindersUseCase(dentistryRepositoryMock, patientRepositoryMock)

    private val today = LocalDate(2025, 1, 15)

    private val patient =
        Patient(
            id = 1L,
            name = "Horse 1",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val vaccination =
        Vaccination(
            id = 1L,
            patientId = patient.id,
            vaccineName = "Tetanus",
            dateAdministered = today,
            nextDueDate = today.plus(DatePeriod(days = 30)),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val dentistry =
        Dentistry(
            id = 1L,
            patientId = patient.id,
            date = today,
            nextDueDate = today.plus(DatePeriod(days = 90)),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ReminderSettingsViewModel(
            getVaccinationRemindersUseCase,
            getDentistryRemindersUseCase,
            dispatcher,
            permissionControllerMock,
        )

    @Test
    fun `check reminders aggregates vaccination and dentistry`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientList() } returns listOf(patient)
            every { vaccinationRepositoryMock.getByPatient(patient.id) } returns listOf(vaccination)
            every { dentistryRepositoryMock.getByPatient(patient.id) } returns listOf(dentistry)
            every { permissionControllerMock.isGranted() } returns true
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.setRemindersEnabled(false)
            advanceUntilIdle()

            vm.checkRemindersNow()
            advanceUntilIdle()

            assertEquals(2, vm.uiState.value.lastCheckedCount)
            assertFalse(vm.uiState.value.isChecking)
            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `toggle updates reminders enabled`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns true
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            assertTrue(vm.uiState.value.remindersEnabled)

            vm.setRemindersEnabled(false)

            assertFalse(vm.uiState.value.remindersEnabled)

            vm.setRemindersEnabled(true)

            assertTrue(vm.uiState.value.remindersEnabled)
        }

    @Test
    fun `repository failure surfaces error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns true
            every { patientRepositoryMock.getPatientList() } throws RuntimeException("database down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.checkRemindersNow()
            advanceUntilIdle()

            assertEquals("database down", vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.lastCheckedCount)
            assertFalse(vm.uiState.value.isChecking)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `init reflects denied permission and keeps reminders disabled`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns false
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            assertFalse(vm.uiState.value.remindersEnabled)
            assertFalse(vm.uiState.value.isPermissionRequesting)
            assertEquals(false, vm.uiState.value.notificationsEnabled)
        }

    @Test
    fun `enable toggle requests permission when not granted`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns false
            everySuspend { permissionControllerMock.request() } returns true
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.setRemindersEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.remindersEnabled)
            assertEquals(true, vm.uiState.value.notificationsEnabled)
            assertNull(vm.uiState.value.permissionMessage)
            verifySuspend(VerifyMode.exactly(1)) { permissionControllerMock.request() }
        }

    @Test
    fun `denied permission keeps toggle disabled with message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns false
            everySuspend { permissionControllerMock.request() } returns false
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.setRemindersEnabled(true)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.remindersEnabled)
            assertFalse(vm.uiState.value.isPermissionRequesting)
            assertEquals(false, vm.uiState.value.notificationsEnabled)
            assertNotNull(vm.uiState.value.permissionMessage)
        }

    @Test
    fun `toggle does not request permission when already granted`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns true
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.setRemindersEnabled(true)
            advanceUntilIdle()

            verifySuspend(VerifyMode.exactly(0)) { permissionControllerMock.request() }
        }

    @Test
    fun `check reminders still works when permission denied`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { permissionControllerMock.isGranted() } returns false
            every { patientRepositoryMock.getPatientList() } returns listOf(patient)
            every { vaccinationRepositoryMock.getByPatient(patient.id) } returns listOf(vaccination)
            every { dentistryRepositoryMock.getByPatient(patient.id) } returns listOf(dentistry)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.checkRemindersNow()
            advanceUntilIdle()

            assertEquals(2, vm.uiState.value.lastCheckedCount)
            assertNull(vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isChecking)
        }
}
