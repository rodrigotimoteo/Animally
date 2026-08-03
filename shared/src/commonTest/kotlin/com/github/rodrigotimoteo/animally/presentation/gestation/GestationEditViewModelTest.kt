package com.github.rodrigotimoteo.animally.presentation.gestation

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GetGestationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.SaveGestationUseCase
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GestationEditViewModelTest {
    private val gestationRepositoryMock: IGestationRepository = mock()

    private val getGestationDetailUseCase = GetGestationDetailUseCase(gestationRepositoryMock)

    private val saveGestationUseCase =
        SaveGestationUseCase(
            gestationRepositoryMock,
            CalculateGestationUseCase(),
        )

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        GestationEditViewModel(
            patientId = 1L,
            gestationId = null,
            getGestationDetailUseCase = getGestationDetailUseCase,
            saveGestationUseCase = saveGestationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank breeding date sets breedingDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onStatusChange("Active")
            vm.save()

            assertEquals("Breeding date is required", vm.formState.value?.breedingDateError)
            verify(VerifyMode.exactly(0)) { gestationRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank status sets statusError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onBreedingDateChange("2026-01-01")
            vm.save()

            assertEquals("Status is required", vm.formState.value?.statusError)
            verify(VerifyMode.exactly(0)) { gestationRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid fetal count sets fetalCountError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onBreedingDateChange("2026-01-01")
            vm.onStatusChange("Active")
            vm.onFetalCountChange("twins")
            vm.save()

            assertEquals("Fetal count must be a whole number", vm.formState.value?.fetalCountError)
            verify(VerifyMode.exactly(0)) { gestationRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves gestation with computed due date and day count and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { gestationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            vm.onBreedingDateChange("2026-01-01")
            vm.onStatusChange("Active")
            vm.onFetalCountChange("2")
            vm.save()
            advanceUntilIdle()

            val expectedDueDate = LocalDate(2026, 1, 1).plus(DatePeriod(days = 340))
            val gestationDays = LocalDate(2026, 1, 1).daysUntil(today).coerceAtLeast(0)
            verify(VerifyMode.exactly(1)) {
                gestationRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.breedingDate == LocalDate(2026, 1, 1) &&
                            it.status == "Active" &&
                            it.fetalCount == 2 &&
                            it.expectedDueDate == expectedDueDate &&
                            it.gestationDays == gestationDays
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `invalid last check date sets lastCheckDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onBreedingDateChange("2026-01-01")
            vm.onStatusChange("Active")
            vm.onLastCheckDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.lastCheckDateError)
            verify(VerifyMode.exactly(0)) { gestationRepositoryMock.insert(any()) }
        }

    @Test
    fun `last check date change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onLastCheckDateChange("2026-02-01")

            assertEquals("2026-02-01", vm.formState.value?.lastCheckDate)

            vm.onLastCheckDateChange("  ")

            assertEquals(null, vm.formState.value?.lastCheckDate)
        }

    @Test
    fun `notes change stores value and blank input stores null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNotesChange("Progressing normally")

            assertEquals("Progressing normally", vm.formState.value?.notes)

            vm.onNotesChange(" ")

            assertEquals(null, vm.formState.value?.notes)
        }

    @Test
    fun `future breeding date saves gestation with zero gestation days`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { gestationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val breedingDate = today.plus(DatePeriod(days = 10))

            vm.onBreedingDateChange(breedingDate.toString())
            vm.onStatusChange("Active")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                gestationRepositoryMock.insert(
                    matches {
                        it.breedingDate == breedingDate &&
                            it.gestationDays == 0 &&
                            it.expectedDueDate == breedingDate.plus(DatePeriod(days = 340))
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `save failure resets isSaving and keeps navigator`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { gestationRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onBreedingDateChange("2026-01-01")
            vm.onStatusChange("Active")
            vm.save()
            advanceUntilIdle()

            assertEquals(false, vm.formState.value?.isSaving)
            assertEquals("boom", vm.formState.value?.breedingDateError)
            assertTrue(navigator.backStack.isNotEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded gestation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gestation =
                Gestation(
                    id = 1L,
                    patientId = 1L,
                    breedingDate = LocalDate(2026, 1, 1),
                    expectedDueDate = LocalDate(2026, 12, 7),
                    gestationDays = 30,
                    status = "Active",
                    fetalCount = 1,
                    lastCheckDate = LocalDate(2026, 2, 1),
                    notes = "Progressing normally",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { gestationRepositoryMock.getById(1L) } returns gestation
            val vm =
                GestationEditViewModel(
                    patientId = 1L,
                    gestationId = 1L,
                    getGestationDetailUseCase = getGestationDetailUseCase,
                    saveGestationUseCase = saveGestationUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                GestationFormState(
                    id = 1L,
                    breedingDate = "2026-01-01",
                    status = "Active",
                    fetalCount = "1",
                    lastCheckDate = "2026-02-01",
                    notes = "Progressing normally",
                    createdAt = gestation.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
