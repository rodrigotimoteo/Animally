package com.github.rodrigotimoteo.animally.presentation.ultrasound

import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.GetUltrasoundDetailUseCase
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.SaveUltrasoundUseCase
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class UltrasoundEditViewModelTest {
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    private val getUltrasoundDetailUseCase = GetUltrasoundDetailUseCase(ultrasoundRepositoryMock)

    private val saveUltrasoundUseCase = SaveUltrasoundUseCase(ultrasoundRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        dispatcher: TestDispatcher,
        saveFile: (fileName: String, bytes: ByteArray) -> String = { name, _ -> "/saved/$name" },
    ) = UltrasoundEditViewModel(
        patientId = 1L,
        ultrasoundId = null,
        getUltrasoundDetailUseCase = getUltrasoundDetailUseCase,
        saveUltrasoundUseCase = saveUltrasoundUseCase,
        animallyNavigator = navigator,
        ioDispatcher = dispatcher,
        saveFile = saveFile,
    )

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { ultrasoundRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid follicle size sets follicleSizeMmError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onFollicleSizeMmChange("abc")
            vm.save()

            assertEquals("Follicle size must be a positive number", vm.formState.value?.follicleSizeMmError)
            verify(VerifyMode.exactly(0)) { ultrasoundRepositoryMock.insert(any()) }
        }

    @Test
    fun `non-positive follicle size sets follicleSizeMmError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onFollicleSizeMmChange("0")
            vm.save()

            assertEquals("Follicle size must be a positive number", vm.formState.value?.follicleSizeMmError)
            verify(VerifyMode.exactly(0)) { ultrasoundRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves ultrasound with parsed follicle size and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ultrasoundRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onOvaryStatusChange("Active")
            vm.onFollicleSizeMmChange("32.5")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                ultrasoundRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.ovaryStatus == "Active" &&
                            it.follicleSizeMm == 32.5
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded ultrasound`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val ultrasound =
                Ultrasound(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    ovaryStatus = "Active",
                    uterineStatus = "Edematous",
                    follicleSizeMm = 32.5,
                    findings = "Follicle developing",
                    vetName = "Dr. X",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { ultrasoundRepositoryMock.getById(1L) } returns ultrasound
            val vm =
                UltrasoundEditViewModel(
                    patientId = 1L,
                    ultrasoundId = 1L,
                    getUltrasoundDetailUseCase = getUltrasoundDetailUseCase,
                    saveUltrasoundUseCase = saveUltrasoundUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                UltrasoundFormState(
                    id = 1L,
                    date = "2026-01-15",
                    ovaryStatus = "Active",
                    uterineStatus = "Edematous",
                    follicleSizeMm = "32.5",
                    findings = "Follicle developing",
                    vetName = "Dr. X",
                    createdAt = ultrasound.createdAt,
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
}
