package com.github.rodrigotimoteo.animally.presentation.dentistry

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.GetDentistryDetailUseCase
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.SaveDentistryUseCase
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
class DentistryEditViewModelTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private val getDentistryDetailUseCase = GetDentistryDetailUseCase(dentistryRepositoryMock)

    private val saveDentistryUseCase = SaveDentistryUseCase(dentistryRepositoryMock, FakeSearchRepository())

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        DentistryEditViewModel(
            patientId = 1L,
            dentistryId = null,
            getDentistryDetailUseCase = getDentistryDetailUseCase,
            saveDentistryUseCase = saveDentistryUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { dentistryRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { dentistryRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves dentistry with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onFindingsChange("Mild tartar")
            vm.onTreatmentChange("Floating")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                dentistryRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.findings == "Mild tartar" &&
                            it.treatment == "Floating"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `invalid next due date sets nextDueDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onNextDueDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.nextDueDateError)
            verify(VerifyMode.exactly(0)) { dentistryRepositoryMock.insert(any()) }
        }

    @Test
    fun `vet name change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVetNameChange("Dr. X")

            assertEquals("Dr. X", vm.formState.value?.vetName)

            vm.onVetNameChange("  ")

            assertEquals(null, vm.formState.value?.vetName)
        }

    @Test
    fun `notes change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNotesChange("Follow up in 6 months")

            assertEquals("Follow up in 6 months", vm.formState.value?.notes)

            vm.onNotesChange(" ")

            assertEquals(null, vm.formState.value?.notes)
        }

    @Test
    fun `schedule selection clears error and saves selected next due date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onNextDueDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.nextDueDateError)

            vm.onNextDueDateChange("2026-07-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            assertEquals(null, vm.formState.value?.nextDueDateError)
            verify(VerifyMode.exactly(1)) {
                dentistryRepositoryMock.insert(
                    matches { it.nextDueDate == LocalDate(2026, 7, 15) },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `save failure resets isSaving and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            assertEquals(false, vm.formState.value?.isSaving)
            assertEquals("boom", vm.formState.value?.dateError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded dentistry record`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val record =
                Dentistry(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    findings = "Mild tartar",
                    treatment = "Floating",
                    nextDueDate = LocalDate(2026, 7, 15),
                    vetName = "Dr. X",
                    notes = "Follow up in 6 months",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { dentistryRepositoryMock.getById(1L) } returns record
            val vm =
                DentistryEditViewModel(
                    patientId = 1L,
                    dentistryId = 1L,
                    getDentistryDetailUseCase = getDentistryDetailUseCase,
                    saveDentistryUseCase = saveDentistryUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                DentistryFormState(
                    id = 1L,
                    date = "2026-01-15",
                    findings = "Mild tartar",
                    treatment = "Floating",
                    nextDueDate = "2026-07-15",
                    vetName = "Dr. X",
                    notes = "Follow up in 6 months",
                    createdAt = record.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
