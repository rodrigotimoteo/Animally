package com.github.rodrigotimoteo.animally.presentation.surgery

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.GetSurgeryDetailUseCase
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.SaveSurgeryUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SurgeryEditViewModelTest {
    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private val getSurgeryDetailUseCase = GetSurgeryDetailUseCase(surgeryRepositoryMock)

    private val saveSurgeryUseCase = SaveSurgeryUseCase(surgeryRepositoryMock, FakeSearchRepository())

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        SurgeryEditViewModel(
            patientId = 1L,
            surgeryId = null,
            getSurgeryDetailUseCase = getSurgeryDetailUseCase,
            saveSurgeryUseCase = saveSurgeryUseCase,
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
            verify(VerifyMode.exactly(0)) { surgeryRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { surgeryRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves surgery with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { surgeryRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onTypeChange("Colic surgery")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                surgeryRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.type == "Colic surgery"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded surgery`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val surgery =
                Surgery(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    type = "Colic surgery",
                    surgeon = "Dr. Y",
                    outcome = "Recovered",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { surgeryRepositoryMock.getById(1L) } returns surgery
            val vm =
                SurgeryEditViewModel(
                    patientId = 1L,
                    surgeryId = 1L,
                    getSurgeryDetailUseCase = getSurgeryDetailUseCase,
                    saveSurgeryUseCase = saveSurgeryUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                SurgeryFormState(
                    id = 1L,
                    date = "2026-01-15",
                    type = "Colic surgery",
                    surgeon = "Dr. Y",
                    outcome = "Recovered",
                    createdAt = surgery.createdAt,
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

            vm.onDescriptionChange("Enterotomy")
            vm.onOutcomeChange("Recovered")
            vm.onSurgeonChange("Dr. Y")
            vm.onAnesthesiaChange("General")
            vm.onAnalgesiaChange("Flunixin")
            vm.onComplicationsChange("None")
            vm.onRecoveryNotesChange("Smooth")

            val form = assertNotNull(vm.formState.value)
            assertEquals("Enterotomy", form.description)
            assertEquals("Recovered", form.outcome)
            assertEquals("Dr. Y", form.surgeon)
            assertEquals("General", form.anesthesia)
            assertEquals("Flunixin", form.analgesia)
            assertEquals("None", form.complications)
            assertEquals("Smooth", form.recoveryNotes)

            vm.onDescriptionChange("")
            vm.onOutcomeChange("")
            vm.onSurgeonChange("")
            vm.onAnesthesiaChange("")
            vm.onAnalgesiaChange("")
            vm.onComplicationsChange("")
            vm.onRecoveryNotesChange("")

            val cleared = assertNotNull(vm.formState.value)
            assertEquals(null, cleared.description)
            assertEquals(null, cleared.outcome)
            assertEquals(null, cleared.surgeon)
            assertEquals(null, cleared.anesthesia)
            assertEquals(null, cleared.analgesia)
            assertEquals(null, cleared.complications)
            assertEquals(null, cleared.recoveryNotes)
        }

    @Test
    fun `save failure resets isSaving and sets dateError and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { surgeryRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.dateError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
