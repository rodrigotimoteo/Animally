package com.github.rodrigotimoteo.animally.presentation.vaccination

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.GetVaccinationsByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class VaccinationListViewModelTest {
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private val getVaccinationsByPatientUseCase = GetVaccinationsByPatientUseCase(vaccinationRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val vaccination =
        Vaccination(
            id = 1L,
            patientId = 1L,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate(2026, 1, 15),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        VaccinationListViewModel(
            patientId = 1L,
            getVaccinationsByPatientUseCase = getVaccinationsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads vaccinations on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vaccinations = listOf(vaccination)
            every { vaccinationRepositoryMock.getByPatient(1L) } returns vaccinations
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(vaccinations, vm.uiState.value.vaccinations)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add vaccination`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { vaccinationRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditVaccination(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit vaccination`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { vaccinationRepositoryMock.getByPatient(1L) } returns listOf(vaccination)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(vaccination.id)

            assertEquals(Route.AddEditVaccination(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { vaccinationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
