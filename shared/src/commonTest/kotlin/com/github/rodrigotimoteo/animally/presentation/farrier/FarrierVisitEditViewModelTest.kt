package com.github.rodrigotimoteo.animally.presentation.farrier

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.GetFarrierVisitDetailUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.SaveFarrierVisitUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
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
class FarrierVisitEditViewModelTest {
    private val farrierVisitRepositoryMock: IFarrierVisitRepository = mock()

    private val getFarrierVisitDetailUseCase = GetFarrierVisitDetailUseCase(farrierVisitRepositoryMock)

    private val saveFarrierVisitUseCase = SaveFarrierVisitUseCase(farrierVisitRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        FarrierVisitEditViewModel(
            patientId = 1L,
            farrierVisitId = null,
            getFarrierVisitDetailUseCase = getFarrierVisitDetailUseCase,
            saveFarrierVisitUseCase = saveFarrierVisitUseCase,
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
            verify(VerifyMode.exactly(0)) { farrierVisitRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { farrierVisitRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves farrier visit with parsed date and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { farrierVisitRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onTrimOrShoeChange("Shoe")
            vm.onShoeTypeChange("Steel")
            vm.onFarrierChange("Jane")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                farrierVisitRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.trimOrShoe == "Shoe" &&
                            it.shoeType == "Steel" &&
                            it.farrier == "Jane"
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
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
            verify(VerifyMode.exactly(0)) { farrierVisitRepositoryMock.insert(any()) }
        }

    @Test
    fun `findings change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onFindingsChange("Balanced hooves")

            assertEquals("Balanced hooves", vm.formState.value?.findings)

            vm.onFindingsChange("  ")

            assertEquals(null, vm.formState.value?.findings)
        }

    @Test
    fun `notes change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNotesChange("Reset shoes")

            assertEquals("Reset shoes", vm.formState.value?.notes)

            vm.onNotesChange(" ")

            assertEquals(null, vm.formState.value?.notes)
        }

    @Test
    fun `save failure resets isSaving and keeps navigator`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { farrierVisitRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.save()
            advanceUntilIdle()

            assertEquals(false, vm.formState.value?.isSaving)
            assertEquals("boom", vm.formState.value?.dateError)
            assertTrue(navigator.backStack.isNotEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded farrier visit`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val visit =
                FarrierVisit(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    trimOrShoe = "Shoe",
                    shoeType = "Steel",
                    findings = "Balanced hooves",
                    nextDueDate = LocalDate(2026, 2, 15),
                    farrier = "Jane",
                    notes = "Reset shoes",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { farrierVisitRepositoryMock.getById(1L) } returns visit
            val vm =
                FarrierVisitEditViewModel(
                    patientId = 1L,
                    farrierVisitId = 1L,
                    getFarrierVisitDetailUseCase = getFarrierVisitDetailUseCase,
                    saveFarrierVisitUseCase = saveFarrierVisitUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                FarrierVisitFormState(
                    id = 1L,
                    date = "2026-01-15",
                    trimOrShoe = "Shoe",
                    shoeType = "Steel",
                    findings = "Balanced hooves",
                    nextDueDate = "2026-02-15",
                    farrier = "Jane",
                    notes = "Reset shoes",
                    createdAt = visit.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
