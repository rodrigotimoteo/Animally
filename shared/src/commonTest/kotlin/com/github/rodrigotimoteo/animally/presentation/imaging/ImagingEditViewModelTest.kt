package com.github.rodrigotimoteo.animally.presentation.imaging

import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingDetailUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.SaveImagingUseCase
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
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun createViewModel(
        dispatcher: TestDispatcher,
        saveFile: (fileName: String, bytes: ByteArray) -> String = { name, _ -> "/saved/$name" },
    ) = ImagingEditViewModel(
        patientId = 1L,
        imagingId = null,
        getImagingDetailUseCase = getImagingDetailUseCase,
        saveImagingUseCase = saveImagingUseCase,
        animallyNavigator = navigator,
        ioDispatcher = dispatcher,
        saveFile = saveFile,
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

    @Test
    fun `onFilesPicked saves picked files and appends their paths to imageUris`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val saved = mutableListOf<Pair<String, ByteArray>>()
            val vm =
                createViewModel(StandardTestDispatcher(testScheduler)) { name, bytes ->
                    saved += name to bytes
                    "/saved/$name"
                }

            vm.onFilesPicked(
                listOf(
                    PickedFile(name = "scan-a.jpg") { byteArrayOf(1, 2, 3) },
                    PickedFile(name = "scan-b.jpg") { byteArrayOf(4, 5) },
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf("scan-a.jpg", "scan-b.jpg"), saved.map { it.first })
            assertContentEquals(byteArrayOf(1, 2, 3), saved[0].second)
            assertContentEquals(byteArrayOf(4, 5), saved[1].second)
            assertEquals("/saved/scan-a.jpg,/saved/scan-b.jpg", vm.formState.value?.imageUris)
        }

    @Test
    fun `onFilesPicked appends to existing imageUris without duplication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onImageUrisChange("/saved/old.jpg")

            vm.onFilesPicked(listOf(PickedFile(name = "scan-c.jpg") { byteArrayOf(9) }))
            advanceUntilIdle()

            assertEquals("/saved/old.jpg,/saved/scan-c.jpg", vm.formState.value?.imageUris)
        }

    @Test
    fun `removeImageUri drops the requested uri and keeps the others`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onImageUrisChange("/saved/a.jpg,/saved/b.jpg")

            vm.removeImageUri("/saved/a.jpg")

            assertEquals("/saved/b.jpg", vm.formState.value?.imageUris)
        }

    @Test
    fun `removeImageUri nulls imageUris when last uri is removed`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onImageUrisChange("/saved/only.jpg")

            vm.removeImageUri("/saved/only.jpg")

            assertEquals(null, vm.formState.value?.imageUris)
        }

    @Test
    fun `optional field setters store values and null out on blank`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVetNameChange("Dr. X")
            vm.onNotesChange("Repeat if symptoms persist")

            val form = assertNotNull(vm.formState.value)
            assertEquals("Dr. X", form.vetName)
            assertEquals("Repeat if symptoms persist", form.notes)

            vm.onVetNameChange("")
            vm.onNotesChange("")

            val cleared = assertNotNull(vm.formState.value)
            assertEquals(null, cleared.vetName)
            assertEquals(null, cleared.notes)
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTypeChange("X-ray")
            vm.onDateChange("01-05-2024")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { imagingRepositoryMock.insert(any()) }
        }

    @Test
    fun `save failure resets isSaving and sets dateError`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { imagingRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onTypeChange("X-ray")
            vm.onDateChange("2024-05-01")
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.dateError)
            assertTrue(navigator.backStack.isNotEmpty())
        }
}
