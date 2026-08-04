package com.github.rodrigotimoteo.animally.presentation.deworming

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.GetDewormingDetailUseCase
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.SaveDewormingUseCase
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
class DewormingEditViewModelTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private val getDewormingDetailUseCase = GetDewormingDetailUseCase(dewormingRepositoryMock)

    private val saveDewormingUseCase = SaveDewormingUseCase(dewormingRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        DewormingEditViewModel(
            patientId = 1L,
            dewormingId = null,
            getDewormingDetailUseCase = getDewormingDetailUseCase,
            saveDewormingUseCase = saveDewormingUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank product sets productError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.save()

            assertEquals("Product is required", vm.formState.value?.productError)
            verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onProductChange("Ivermectin")
            vm.onDateAdministeredChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves deworming with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dewormingRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onProductChange("Ivermectin")
            vm.onDateAdministeredChange("2026-01-15")
            vm.onDoseChange("200 mcg/kg")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                dewormingRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.product == "Ivermectin" &&
                            it.dateAdministered == LocalDate(2026, 1, 15) &&
                            it.dose == "200 mcg/kg"
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

            vm.onProductChange("Ivermectin")
            vm.onDateAdministeredChange("2026-01-15")
            vm.onNextDueDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.nextDueDateError)
            verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onProductChange("Ivermectin")
            vm.onDateAdministeredChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.insert(any()) }
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

            vm.onNotesChange("Administered orally")

            assertEquals("Administered orally", vm.formState.value?.notes)

            vm.onNotesChange(" ")

            assertEquals(null, vm.formState.value?.notes)
        }

    @Test
    fun `save failure resets isSaving and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dewormingRepositoryMock.insert(any()) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onProductChange("Ivermectin")
            vm.onDateAdministeredChange("2026-01-15")
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
    fun `edit mode prefills form from loaded deworming`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val deworming =
                Deworming(
                    id = 1L,
                    patientId = 1L,
                    product = "Ivermectin",
                    dateAdministered = LocalDate(2026, 1, 15),
                    nextDueDate = LocalDate(2026, 4, 15),
                    dose = "200 mcg/kg",
                    vetName = "Dr. X",
                    notes = "Administered orally",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { dewormingRepositoryMock.getById(1L) } returns deworming
            val vm =
                DewormingEditViewModel(
                    patientId = 1L,
                    dewormingId = 1L,
                    getDewormingDetailUseCase = getDewormingDetailUseCase,
                    saveDewormingUseCase = saveDewormingUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                DewormingFormState(
                    id = 1L,
                    product = "Ivermectin",
                    dateAdministered = "2026-01-15",
                    nextDueDate = "2026-04-15",
                    dose = "200 mcg/kg",
                    vetName = "Dr. X",
                    notes = "Administered orally",
                    createdAt = deworming.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
