package com.github.rodrigotimoteo.animally.presentation.patientDetail

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PatientDetailViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val getPatientDetailUseCase = GetPatientDetailUseCase(patientRepositoryMock)

    private val getOwnerDetailUseCase = GetOwnerDetailUseCase(ownerRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val owner =
        Owner(
            id = 2L,
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val patient =
        Patient(
            id = 1L,
            name = "Midnight",
            species = "Equine",
            breed = null,
            dateOfBirth = LocalDate(2020, 5, 1),
            ownerId = 2L,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        PatientDetailViewModel(patient.id, getPatientDetailUseCase, getOwnerDetailUseCase, navigator, dispatcher)

    @Test
    fun `load populates patient and owner name`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(patient.id) } returns patient
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(patient, vm.uiState.value.patient)
            assertEquals("Alice", vm.uiState.value.ownerName)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `load with unassigned patient leaves owner name null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val unassigned = patient.copy(ownerId = null)
            every { patientRepositoryMock.getPatientById(patient.id) } returns unassigned
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(unassigned, vm.uiState.value.patient)
            assertNull(vm.uiState.value.ownerName)
            verify(VerifyMode.exactly(0)) { ownerRepositoryMock.getOwnerById(any()) }
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(patient.id) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on edit click navigates to add edit patient`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(patient.id) } returns patient
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick()

            assertEquals(Route.AddEditPatient(patient.id), navigator.backStack.last())
        }

    @Test
    fun `on anamnese click navigates to add edit anamnese`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(patient.id) } returns patient
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAnamneseClick()

            assertEquals(Route.AddEditAnamnese(patient.id), navigator.backStack.last())
        }
}
