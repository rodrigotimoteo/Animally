package com.github.rodrigotimoteo.animally.presentation.ultrasound

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
class UltrasoundEditViewModelTest {
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    private val getUltrasoundDetailUseCase = GetUltrasoundDetailUseCase(ultrasoundRepositoryMock)

    private val saveUltrasoundUseCase = SaveUltrasoundUseCase(ultrasoundRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        UltrasoundEditViewModel(
            patientId = 1L,
            ultrasoundId = null,
            getUltrasoundDetailUseCase = getUltrasoundDetailUseCase,
            saveUltrasoundUseCase = saveUltrasoundUseCase,
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
}
