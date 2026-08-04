package com.github.rodrigotimoteo.animally.presentation.medication

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.medication.usecase.GetMedicationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.medication.usecase.SaveMedicationUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationEditViewModelTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()

    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private val getMedicationDetailUseCase = GetMedicationDetailUseCase(medicationRepositoryMock)

    private val saveMedicationUseCase = SaveMedicationUseCase(medicationRepositoryMock, searchRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        MedicationEditViewModel(
            patientId = 1L,
            medicationId = null,
            getMedicationDetailUseCase = getMedicationDetailUseCase,
            saveMedicationUseCase = saveMedicationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank name sets nameError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDosageChange("2g")
            vm.save()

            assertEquals("Name is required", vm.formState.value?.nameError)
            verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank dosage sets dosageError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Phenylbutazone")
            vm.save()

            assertEquals("Dosage is required", vm.formState.value?.dosageError)
            verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid start date sets startDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Phenylbutazone")
            vm.onDosageChange("2g")
            vm.onStartDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.startDateError)
            verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves medication with parsed dates and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { medicationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Phenylbutazone")
            vm.onDosageChange("2g")
            vm.onStartDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                medicationRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.name == "Phenylbutazone" &&
                            it.dosage == "2g" &&
                            it.startDate == LocalDate(2026, 1, 15) &&
                            it.endDate == null
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val medication =
                Medication(
                    id = 1L,
                    patientId = 1L,
                    name = "Phenylbutazone",
                    dosage = "2g",
                    route = "Oral",
                    frequency = "BID",
                    startDate = LocalDate(2026, 1, 15),
                    endDate = LocalDate(2026, 1, 30),
                    prescribedBy = "Dr. X",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { medicationRepositoryMock.getById(1L) } returns medication
            val vm =
                MedicationEditViewModel(
                    patientId = 1L,
                    medicationId = 1L,
                    getMedicationDetailUseCase = getMedicationDetailUseCase,
                    saveMedicationUseCase = saveMedicationUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                MedicationFormState(
                    id = 1L,
                    name = "Phenylbutazone",
                    dosage = "2g",
                    route = "Oral",
                    frequency = "BID",
                    startDate = "2026-01-15",
                    endDate = "2026-01-30",
                    prescribedBy = "Dr. X",
                    createdAt = medication.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `optional field setters store values and null out on blank`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onRouteChange("Oral")
            vm.onFrequencyChange("BID")
            vm.onEndDateChange("2026-01-30")
            vm.onPrescribedByChange("Dr. X")
            vm.onNotesChange("Give with feed")

            val form = assertNotNull(vm.formState.value)
            assertEquals("Oral", form.route)
            assertEquals("BID", form.frequency)
            assertEquals("2026-01-30", form.endDate)
            assertEquals("Dr. X", form.prescribedBy)
            assertEquals("Give with feed", form.notes)

            vm.onRouteChange("")
            vm.onFrequencyChange("")
            vm.onEndDateChange("")
            vm.onPrescribedByChange("")
            vm.onNotesChange("")

            val cleared = assertNotNull(vm.formState.value)
            assertEquals(null, cleared.route)
            assertEquals(null, cleared.frequency)
            assertEquals(null, cleared.endDate)
            assertEquals(null, cleared.prescribedBy)
            assertEquals(null, cleared.notes)
        }

    @Test
    fun `invalid end date sets endDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Phenylbutazone")
            vm.onDosageChange("2g")
            vm.onEndDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.endDateError)
            verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        }

    @Test
    fun `save failure resets isSaving and sets nameError and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { medicationRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNameChange("Phenylbutazone")
            vm.onDosageChange("2g")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.nameError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
