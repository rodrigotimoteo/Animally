package com.github.rodrigotimoteo.animally.presentation.patientEdit

import app.cash.turbine.test
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.SavePatientUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
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

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        PatientEditViewModel(
            null,
            getPatientDetailUseCase,
            savePatientUseCase,
            getOwnerListUseCase,
            navigator,
            dispatcher,
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

    @Test
    fun `onSpeciesChange updates species`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onSpeciesChange("Feline")

            assertEquals("Feline", vm.formState.value?.species)
        }

    @Test
    fun `optional field setters store values and null out on blank`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onBreedChange("Lusitano")
            vm.onDateOfBirthChange("2020-05-01")
            vm.onGenderChange("Mare")
            vm.onMicrochipIdChange("981000123456789")
            vm.onRegistrationNumberChange("ABC-123")
            vm.onStableLocationChange("Stable A")
            vm.onPhotoUriChange("file://photo.jpg")
            vm.onNotesChange("Friendly")

            val form = assertNotNull(vm.formState.value)
            assertEquals("Lusitano", form.breed)
            assertEquals("2020-05-01", form.dateOfBirth)
            assertEquals("Mare", form.gender)
            assertEquals("981000123456789", form.microchipId)
            assertEquals("ABC-123", form.registrationNumber)
            assertEquals("Stable A", form.stableLocation)
            assertEquals("file://photo.jpg", form.photoUri)
            assertEquals("Friendly", form.notes)

            vm.onBreedChange("")
            vm.onDateOfBirthChange("")
            vm.onGenderChange("")
            vm.onMicrochipIdChange("")
            vm.onRegistrationNumberChange("")
            vm.onStableLocationChange("")
            vm.onPhotoUriChange("")
            vm.onNotesChange("")

            val cleared = assertNotNull(vm.formState.value)
            assertEquals(null, cleared.breed)
            assertEquals(null, cleared.dateOfBirth)
            assertEquals(null, cleared.gender)
            assertEquals(null, cleared.microchipId)
            assertEquals(null, cleared.registrationNumber)
            assertEquals(null, cleared.stableLocation)
            assertEquals(null, cleared.photoUri)
            assertEquals(null, cleared.notes)
        }

    @Test
    fun `onOwnerChange updates ownerId`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onOwnerChange(3L)
            assertEquals(3L, vm.formState.value?.ownerId)

            vm.onOwnerChange(null)
            assertEquals(null, vm.formState.value?.ownerId)
        }

    @Test
    fun `valid 15 digit ueln passes validation and saves`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.insertPatient(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Midnight")
            vm.onUelnChange("826000000000123")
            vm.save()
            advanceUntilIdle()

            assertEquals(null, vm.formState.value?.uelnError)
            verify(VerifyMode.exactly(1)) {
                patientRepositoryMock.insertPatient(matches { it.ueln == "826000000000123" })
            }
        }

    @Test
    fun `edit mode load failure sets nameError`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.getPatientById(1L) } throws RuntimeException("boom")
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

            assertEquals(PatientFormState(id = 1L, nameError = "boom"), vm.formState.value)
        }

    @Test
    fun `save failure resets isSaving and sets nameError`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { patientRepositoryMock.insertPatient(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Midnight")
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.nameError)
            assertTrue(navigator.backStack.isNotEmpty())
        }

    @Test
    fun `owners are loaded into owners state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owners =
                listOf(
                    Owner(
                        id = 1L,
                        name = "Bob",
                        email = null,
                        phone = "123",
                        address = null,
                        createdAt = Instant.fromEpochMilliseconds(0L),
                        updatedAt = Instant.fromEpochMilliseconds(0L),
                    ),
                )
            every { ownerRepositoryMock.getOwnerList() } returns owners
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(owners, vm.owners.value)
        }
}
