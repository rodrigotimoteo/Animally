package com.github.rodrigotimoteo.animally.presentation.lameness

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.GetLamenessDetailUseCase
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.SaveLamenessUseCase
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
class LamenessEditViewModelTest {
    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private val getLamenessDetailUseCase = GetLamenessDetailUseCase(lamenessRepositoryMock)

    private val saveLamenessUseCase = SaveLamenessUseCase(lamenessRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        LamenessEditViewModel(
            patientId = 1L,
            lamenessId = null,
            getLamenessDetailUseCase = getLamenessDetailUseCase,
            saveLamenessUseCase = saveLamenessUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { lamenessRepositoryMock.insert(any()) }
        }

    @Test
    fun `grade out of range sets gradeError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onGradeAAEPChange("7")
            vm.save()

            assertEquals("Grade must be 1-5", vm.formState.value?.gradeError)
            verify(VerifyMode.exactly(0)) { lamenessRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves lameness with parsed date and grade and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { lamenessRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onGradeAAEPChange("3")
            vm.onDiagnosisChange("Suspensory desmitis")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                lamenessRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.gradeAAEP == 3 &&
                            it.diagnosis == "Suspensory desmitis"
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded lameness`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val lameness =
                Lameness(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    gradeAAEP = 3,
                    limbLocation = "Left hind",
                    diagnosis = "Suspensory desmitis",
                    vetName = "Dr. X",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { lamenessRepositoryMock.getById(1L) } returns lameness
            val vm =
                LamenessEditViewModel(
                    patientId = 1L,
                    lamenessId = 1L,
                    getLamenessDetailUseCase = getLamenessDetailUseCase,
                    saveLamenessUseCase = saveLamenessUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                LamenessFormState(
                    id = 1L,
                    date = "2026-01-15",
                    gradeAAEP = "3",
                    limbLocation = "Left hind",
                    diagnosis = "Suspensory desmitis",
                    vetName = "Dr. X",
                    createdAt = lameness.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
