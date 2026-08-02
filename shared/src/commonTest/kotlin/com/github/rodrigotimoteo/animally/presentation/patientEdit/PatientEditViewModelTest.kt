package com.github.rodrigotimoteo.animally.presentation.patientEdit

import app.cash.turbine.test
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.SavePatientUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PatientEditViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private val getPatientDetailUseCase = GetPatientDetailUseCase(patientRepositoryMock)

    private val savePatientUseCase = SavePatientUseCase(patientRepositoryMock, searchRepositoryMock)

    private val getOwnerListUseCase = GetOwnerListUseCase(ownerRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val patient =
        Patient(
            id = 1L,
            name = "Midnight",
            species = "Equine",
            breed = "Lusitano",
            dateOfBirth = LocalDate(2020, 5, 1),
            microchipId = "981000123456789",
            cogginsTestDate = LocalDate(2025, 1, 10),
            cogginsResult = "Negative",
            cogginsExpiryDate = LocalDate(2025, 7, 10),
            ownerId = 2L,
            createdAt = Instant.fromEpochMilliseconds(100L),
            updatedAt = Instant.fromEpochMilliseconds(100L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create mode with blank name sets nameError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm =
                PatientEditViewModel(
                    null,
                    getPatientDetailUseCase,
                    savePatientUseCase,
                    getOwnerListUseCase,
                    navigator,
                    StandardTestDispatcher(testScheduler),
                )

            vm.formState.test {
                assertEquals(PatientFormState(), awaitItem())

                vm.save()

                assertEquals(PatientFormState(nameError = "Name is required"), awaitItem())
            }

            verify(VerifyMode.exactly(0)) { patientRepositoryMock.insertPatient(any()) }
        }

    @Test
    fun `create mode with valid name inserts patient and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.insertPatient(any()) } returns 1L
            val vm =
                PatientEditViewModel(
                    null,
                    getPatientDetailUseCase,
                    savePatientUseCase,
                    getOwnerListUseCase,
                    navigator,
                    StandardTestDispatcher(testScheduler),
                )

            vm.onNameChange("Midnight")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) { patientRepositoryMock.insertPatient(any()) }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `invalid ueln sets uelnError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm =
                PatientEditViewModel(
                    null,
                    getPatientDetailUseCase,
                    savePatientUseCase,
                    getOwnerListUseCase,
                    navigator,
                    StandardTestDispatcher(testScheduler),
                )

            vm.onNameChange("Midnight")
            vm.onUelnChange("12345")
            vm.save()

            assertEquals("UELN must be 15 digits", vm.formState.value?.uelnError)
            verify(VerifyMode.exactly(0)) { patientRepositoryMock.insertPatient(any()) }
        }

    @Test
    fun `edit mode prefills form from loaded patient`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(1L) } returns patient
            val vm =
                PatientEditViewModel(
                    1L,
                    getPatientDetailUseCase,
                    savePatientUseCase,
                    getOwnerListUseCase,
                    navigator,
                    StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                PatientFormState(
                    id = 1L,
                    name = "Midnight",
                    breed = "Lusitano",
                    dateOfBirth = "2020-05-01",
                    microchipId = "981000123456789",
                    cogginsTestDate = "2025-01-10",
                    cogginsResult = "Negative",
                    cogginsExpiryDate = "2025-07-10",
                    ownerId = 2L,
                    createdAt = patient.createdAt,
                ),
                vm.formState.value,
            )
            assertFalse(assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `edit mode saves with loaded patient id`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(1L) } returns patient
            every { patientRepositoryMock.updatePatient(any()) } returns 1L
            val vm =
                PatientEditViewModel(
                    1L,
                    getPatientDetailUseCase,
                    savePatientUseCase,
                    getOwnerListUseCase,
                    navigator,
                    StandardTestDispatcher(testScheduler),
                )
            advanceUntilIdle()

            vm.onNameChange("Midnight Updated")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                patientRepositoryMock.updatePatient(matches { it.id == 1L && it.name == "Midnight Updated" })
            }
            assertTrue(navigator.backStack.isEmpty())
        }
}
