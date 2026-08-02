package com.github.rodrigotimoteo.animally.presentation.imaging

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingDetailUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.SaveImagingUseCase
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
class ImagingEditViewModelTest {
    private val imagingRepositoryMock: IImagingRepository = mock()

    private val getImagingDetailUseCase = GetImagingDetailUseCase(imagingRepositoryMock)

    private val saveImagingUseCase = SaveImagingUseCase(imagingRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        ImagingEditViewModel(
            patientId = 1L,
            imagingId = null,
            getImagingDetailUseCase = getImagingDetailUseCase,
            saveImagingUseCase = saveImagingUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank type sets typeError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTypeChange("")
            vm.onDateChange("2024-05-01")
            vm.save()

            assertEquals("Type is required", vm.formState.value?.typeError)
            verify(VerifyMode.exactly(0)) { imagingRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTypeChange("X-ray")
            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { imagingRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves imaging with parsed date and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTypeChange("X-ray")
            vm.onDateChange("2024-05-01")
            vm.onFindingsChange("No acute findings")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                imagingRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.type == "X-ray" &&
                            it.date == LocalDate(2024, 5, 1) &&
                            it.findings == "No acute findings"
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded imaging`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val imaging =
                Imaging(
                    id = 1L,
                    patientId = 1L,
                    type = "X-ray",
                    date = LocalDate(2024, 5, 1),
                    findings = "No acute findings",
                    imageUris = "file://a.jpg,file://b.jpg",
                    vetName = "Dr. X",
                    notes = "Repeat if symptoms persist",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { imagingRepositoryMock.getById(1L) } returns imaging
            val vm =
                ImagingEditViewModel(
                    patientId = 1L,
                    imagingId = 1L,
                    getImagingDetailUseCase = getImagingDetailUseCase,
                    saveImagingUseCase = saveImagingUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                ImagingFormState(
                    id = 1L,
                    type = "X-ray",
                    date = "2024-05-01",
                    findings = "No acute findings",
                    imageUris = "file://a.jpg,file://b.jpg",
                    vetName = "Dr. X",
                    notes = "Repeat if symptoms persist",
                    createdAt = imaging.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
