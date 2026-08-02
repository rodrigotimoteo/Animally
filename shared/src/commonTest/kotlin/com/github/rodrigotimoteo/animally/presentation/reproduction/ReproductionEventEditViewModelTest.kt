package com.github.rodrigotimoteo.animally.presentation.reproduction

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.GetReproductionEventDetailUseCase
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.SaveReproductionEventUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import dev.mokkery.answering.returns
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
class ReproductionEventEditViewModelTest {
    private val reproductionRepositoryMock: IReproductionRepository = mock()

    private val getReproductionEventDetailUseCase = GetReproductionEventDetailUseCase(reproductionRepositoryMock)

    private val saveReproductionEventUseCase = SaveReproductionEventUseCase(reproductionRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        ReproductionEventEditViewModel(
            patientId = 1L,
            reproductionEventId = null,
            getReproductionEventDetailUseCase = getReproductionEventDetailUseCase,
            saveReproductionEventUseCase = saveReproductionEventUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank event type sets eventTypeError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.save()

            assertEquals("Event type is required", vm.formState.value?.eventTypeError)
            verify(VerifyMode.exactly(0)) { reproductionRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onEventTypeChange("Heat")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { reproductionRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves event with parsed date and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproductionRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onEventTypeChange("Breeding")
            vm.onDateChange("2026-01-15")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                reproductionRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.eventType == "Breeding" &&
                            it.date == LocalDate(2026, 1, 15)
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val event =
                ReproductionEvent(
                    id = 1L,
                    patientId = 1L,
                    eventType = "Foaling",
                    date = LocalDate(2026, 1, 15),
                    details = "Healthy foal",
                    vetName = "Dr. X",
                    notes = "Uneventful delivery",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { reproductionRepositoryMock.getById(1L) } returns event
            val vm =
                ReproductionEventEditViewModel(
                    patientId = 1L,
                    reproductionEventId = 1L,
                    getReproductionEventDetailUseCase = getReproductionEventDetailUseCase,
                    saveReproductionEventUseCase = saveReproductionEventUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                ReproductionEventFormState(
                    id = 1L,
                    eventType = "Foaling",
                    date = "2026-01-15",
                    details = "Healthy foal",
                    vetName = "Dr. X",
                    notes = "Uneventful delivery",
                    createdAt = event.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
