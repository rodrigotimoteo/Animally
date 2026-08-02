package com.github.rodrigotimoteo.animally.presentation.labresult

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.GetLabResultDetailUseCase
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.SaveLabResultUseCase
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
class LabResultEditViewModelTest {
    private val labResultRepositoryMock: ILabResultRepository = mock()

    private val getLabResultDetailUseCase = GetLabResultDetailUseCase(labResultRepositoryMock)

    private val saveLabResultUseCase = SaveLabResultUseCase(labResultRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        LabResultEditViewModel(
            patientId = 1L,
            labResultId = null,
            getLabResultDetailUseCase = getLabResultDetailUseCase,
            saveLabResultUseCase = saveLabResultUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank test type sets testTypeError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTestTypeChange("")
            vm.onDateChange("2024-05-01")
            vm.save()

            assertEquals("Test type is required", vm.formState.value?.testTypeError)
            verify(VerifyMode.exactly(0)) { labResultRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTestTypeChange("CBC")
            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { labResultRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves lab result with parsed date and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { labResultRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTestTypeChange("CBC")
            vm.onDateChange("2024-05-01")
            vm.onResultsChange("12.5")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                labResultRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.testType == "CBC" &&
                            it.date == LocalDate(2024, 5, 1) &&
                            it.results == "12.5"
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded lab result`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val labResult =
                LabResult(
                    id = 1L,
                    patientId = 1L,
                    testType = "CBC",
                    date = LocalDate(2024, 5, 1),
                    results = "12.5",
                    normalRange = "8-18",
                    vetName = "Dr. X",
                    notes = "Follow-up in 2 weeks",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { labResultRepositoryMock.getById(1L) } returns labResult
            val vm =
                LabResultEditViewModel(
                    patientId = 1L,
                    labResultId = 1L,
                    getLabResultDetailUseCase = getLabResultDetailUseCase,
                    saveLabResultUseCase = saveLabResultUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                LabResultFormState(
                    id = 1L,
                    testType = "CBC",
                    date = "2024-05-01",
                    results = "12.5",
                    normalRange = "8-18",
                    vetName = "Dr. X",
                    notes = "Follow-up in 2 weeks",
                    createdAt = labResult.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
