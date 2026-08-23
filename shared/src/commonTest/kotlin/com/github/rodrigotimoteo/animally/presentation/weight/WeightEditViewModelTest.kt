package com.github.rodrigotimoteo.animally.presentation.weight

import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.GetWeightDetailUseCase
import com.github.rodrigotimoteo.animally.domain.weight.usecase.SaveWeightUseCase
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
class WeightEditViewModelTest {
    private val weightRepositoryMock: IWeightRepository = mock()

    private val getWeightDetailUseCase = GetWeightDetailUseCase(weightRepositoryMock)

    private val saveWeightUseCase = SaveWeightUseCase(weightRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        WeightEditViewModel(
            patientId = 1L,
            weightId = null,
            getWeightDetailUseCase = getWeightDetailUseCase,
            saveWeightUseCase = saveWeightUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank weight sets weightError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("")
            vm.save()

            assertEquals("Weight is required", vm.formState.value?.weightError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid weight value sets weightError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("abc")
            vm.save()

            assertEquals("Invalid weight", vm.formState.value?.weightError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `non-positive weight sets weightError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("0")
            vm.save()

            assertEquals("Weight must be greater than 0", vm.formState.value?.weightError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves weight with parsed value and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("520.0")
            vm.onDateChange("2024-05-01")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                weightRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.weightKg == 520.0 &&
                            it.date == LocalDate(2024, 5, 1)
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded weight`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val weight =
                Weight(
                    id = 1L,
                    patientId = 1L,
                    weightKg = 520.0,
                    date = LocalDate(2024, 5, 1),
                    notes = "Spring baseline",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { weightRepositoryMock.getById(1L) } returns weight
            val vm =
                WeightEditViewModel(
                    patientId = 1L,
                    weightId = 1L,
                    getWeightDetailUseCase = getWeightDetailUseCase,
                    saveWeightUseCase = saveWeightUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                WeightFormState(
                    id = 1L,
                    weightKg = "520.0",
                    date = "2024-05-01",
                    notes = "Spring baseline",
                    createdAt = weight.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `onNotesChange updates notes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onNotesChange("Spring baseline")

            assertEquals("Spring baseline", vm.formState.value?.notes)
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("520.0")
            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("520.0")
            vm.onDateChange("01-05-2024")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `negative weight sets weightError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("-1")
            vm.save()

            assertEquals("Weight must be greater than 0", vm.formState.value?.weightError)
            verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        }

    @Test
    fun `save failure resets isSaving and sets weightError and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { weightRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onWeightKgChange("520.0")
            vm.onDateChange("2024-05-01")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.weightError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
