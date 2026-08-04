package com.github.rodrigotimoteo.animally.presentation.customreminder

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.GetCustomReminderDetailUseCase
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.SaveCustomReminderUseCase
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CustomReminderEditViewModelTest {
    private val customReminderRepositoryMock: ICustomReminderRepository = mock()

    private val reminderSchedulerMock: ReminderScheduler = mock()

    private val getCustomReminderDetailUseCase = GetCustomReminderDetailUseCase(customReminderRepositoryMock)

    private val saveCustomReminderUseCase = SaveCustomReminderUseCase(customReminderRepositoryMock, reminderSchedulerMock)

    private val navigator = AnimallyNavigator()

    init {
        every { reminderSchedulerMock.schedule(any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        CustomReminderEditViewModel(
            patientId = 1L,
            reminderId = null,
            getCustomReminderDetailUseCase = getCustomReminderDetailUseCase,
            saveCustomReminderUseCase = saveCustomReminderUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank title sets titleError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("   ")
            vm.save()

            assertEquals("Title is required", vm.formState.value?.titleError)
            verify(VerifyMode.exactly(0)) { customReminderRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank due date sets dueDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("Farrier check")
            vm.save()

            assertEquals("Due date is required", vm.formState.value?.dueDateError)
            verify(VerifyMode.exactly(0)) { customReminderRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dueDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("Farrier check")
            vm.onDueDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dueDateError)
            verify(VerifyMode.exactly(0)) { customReminderRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid linked record id sets linkedRecordIdError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("Farrier check")
            vm.onDueDateChange("2026-01-15")
            vm.onLinkedRecordIdChange("not-a-number")
            vm.save()

            assertEquals("Linked record id must be a number", vm.formState.value?.linkedRecordIdError)
            verify(VerifyMode.exactly(0)) { customReminderRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves reminder with parsed fields and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("  Farrier check  ")
            vm.onDueDateChange("2026-01-15")
            vm.onLinkedRecordTypeChange("FarrierVisit")
            vm.onLinkedRecordIdChange("7")
            vm.onNotesChange("Call ahead")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                customReminderRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.title == "Farrier check" &&
                            it.dueDate == LocalDate(2026, 1, 15) &&
                            it.linkedRecordType == "FarrierVisit" &&
                            it.linkedRecordId == 7L &&
                            it.notes == "Call ahead"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `save failure resets isSaving emits no Saved effect and does not schedule`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTitleChange("Farrier check")
            vm.onDueDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            assertEquals(false, vm.formState.value?.isSaving)
            assertEquals("boom", vm.formState.value?.titleError)
            assertEquals(emptyList(), receivedEffects.toList())
            verify(VerifyMode.exactly(0)) { reminderSchedulerMock.schedule(any()) }
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded reminder`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val reminder =
                CustomReminder(
                    id = 1L,
                    patientId = 1L,
                    title = "Booster",
                    dueDate = LocalDate(2026, 1, 15),
                    linkedRecordType = "Vaccination",
                    linkedRecordId = 3L,
                    notes = "Tetanus",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { customReminderRepositoryMock.getById(1L) } returns reminder
            val vm =
                CustomReminderEditViewModel(
                    patientId = 1L,
                    reminderId = 1L,
                    getCustomReminderDetailUseCase = getCustomReminderDetailUseCase,
                    saveCustomReminderUseCase = saveCustomReminderUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                CustomReminderFormState(
                    id = 1L,
                    title = "Booster",
                    dueDate = "2026-01-15",
                    linkedRecordType = "Vaccination",
                    linkedRecordId = "3",
                    notes = "Tetanus",
                    createdAt = reminder.createdAt,
                ),
                vm.formState.value,
            )
        }
}
