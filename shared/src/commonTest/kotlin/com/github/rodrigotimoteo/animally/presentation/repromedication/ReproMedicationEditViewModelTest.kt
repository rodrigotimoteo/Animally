package com.github.rodrigotimoteo.animally.presentation.repromedication

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.GetReproMedicationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.SaveReproMedicationUseCase
import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReproMedicationEditViewModelTest {
    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    private val getReproMedicationDetailUseCase = GetReproMedicationDetailUseCase(reproMedicationRepositoryMock)

    private val saveReproMedicationUseCase = SaveReproMedicationUseCase(reproMedicationRepositoryMock, FakeSearchRepository())

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        ReproMedicationEditViewModel(
            patientId = 1L,
            reproMedId = null,
            getReproMedicationDetailUseCase = getReproMedicationDetailUseCase,
            saveReproMedicationUseCase = saveReproMedicationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank medication sets medicationError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateAdministeredChange("2026-01-15")
            vm.save()

            assertEquals("Medication is required", vm.formState.value?.medicationError)
            verify(VerifyMode.exactly(0)) { reproMedicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onMedicationChange("Regumate")
            vm.onDateAdministeredChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { reproMedicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves medication with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onMedicationChange("Regumate")
            vm.onDateAdministeredChange("2026-01-15")
            vm.onDosageChange("0.044 mg/kg")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                reproMedicationRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.medication == "Regumate" &&
                            it.dateAdministered == LocalDate(2026, 1, 15) &&
                            it.dosage == "0.044 mg/kg"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onMedicationChange("Regumate")
            vm.onDateAdministeredChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { reproMedicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `purpose change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onPurposeChange("Cycle regulation")

            assertEquals("Cycle regulation", vm.formState.value?.purpose)

            vm.onPurposeChange("  ")

            assertEquals(null, vm.formState.value?.purpose)
        }

    @Test
    fun `vet name change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVetNameChange("Dr. X")

            assertEquals("Dr. X", vm.formState.value?.vetName)

            vm.onVetNameChange(" ")

            assertEquals(null, vm.formState.value?.vetName)
        }

    @Test
    fun `notes change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNotesChange("Dose in the morning")

            assertEquals("Dose in the morning", vm.formState.value?.notes)

            vm.onNotesChange("  ")

            assertEquals(null, vm.formState.value?.notes)
        }

    @Test
    fun `save failure resets isSaving and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onMedicationChange("Regumate")
            vm.onDateAdministeredChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            assertEquals(false, vm.formState.value?.isSaving)
            assertEquals("boom", vm.formState.value?.medicationError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val medication =
                ReproMedication(
                    id = 1L,
                    patientId = 1L,
                    medication = "Regumate",
                    dateAdministered = LocalDate(2026, 1, 15),
                    dosage = "0.044 mg/kg",
                    purpose = "Cycle regulation",
                    vetName = "Dr. X",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { reproMedicationRepositoryMock.getById(1L) } returns medication
            val vm =
                ReproMedicationEditViewModel(
                    patientId = 1L,
                    reproMedId = 1L,
                    getReproMedicationDetailUseCase = getReproMedicationDetailUseCase,
                    saveReproMedicationUseCase = saveReproMedicationUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                ReproMedicationFormState(
                    id = 1L,
                    medication = "Regumate",
                    dateAdministered = "2026-01-15",
                    dosage = "0.044 mg/kg",
                    purpose = "Cycle regulation",
                    vetName = "Dr. X",
                    createdAt = medication.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
