package com.github.rodrigotimoteo.animally.presentation.vaccination

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.CalculateNextDueDateUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.GetVaccinationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.SaveVaccinationUseCase
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
class VaccinationEditViewModelTest {
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private val getVaccinationDetailUseCase = GetVaccinationDetailUseCase(vaccinationRepositoryMock)

    private val saveVaccinationUseCase =
        SaveVaccinationUseCase(vaccinationRepositoryMock, CalculateNextDueDateUseCase())

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        VaccinationEditViewModel(
            patientId = 1L,
            vaccinationId = null,
            getVaccinationDetailUseCase = getVaccinationDetailUseCase,
            saveVaccinationUseCase = saveVaccinationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank vaccine name sets vaccineNameError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.save()

            assertEquals("Vaccine name is required", vm.formState.value?.vaccineNameError)
            verify(VerifyMode.exactly(0)) { vaccinationRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVaccineNameChange("Tetanus")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { vaccinationRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves with parsed date and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { vaccinationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVaccineNameChange("Tetanus")
            vm.onDateAdministeredChange("2026-01-15")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                vaccinationRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.vaccineName == "Tetanus" &&
                            it.dateAdministered == LocalDate(2026, 1, 15) &&
                            it.nextDueDate == LocalDate(2027, 1, 15)
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded vaccination`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vaccination =
                Vaccination(
                    id = 1L,
                    patientId = 1L,
                    vaccineName = "Tetanus",
                    dateAdministered = LocalDate(2026, 1, 15),
                    nextDueDate = LocalDate(2027, 1, 15),
                    vetName = "Dr. X",
                    batchNumber = "B123",
                    site = "Neck",
                    notes = "Routine",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { vaccinationRepositoryMock.getById(1L) } returns vaccination
            val vm =
                VaccinationEditViewModel(
                    patientId = 1L,
                    vaccinationId = 1L,
                    getVaccinationDetailUseCase = getVaccinationDetailUseCase,
                    saveVaccinationUseCase = saveVaccinationUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                VaccinationFormState(
                    id = 1L,
                    vaccineName = "Tetanus",
                    dateAdministered = "2026-01-15",
                    vetName = "Dr. X",
                    batchNumber = "B123",
                    site = "Neck",
                    notes = "Routine",
                    nextDueDate = "2027-01-15",
                    createdAt = vaccination.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }
}
