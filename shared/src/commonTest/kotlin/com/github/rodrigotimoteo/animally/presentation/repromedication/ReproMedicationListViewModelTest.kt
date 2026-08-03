package com.github.rodrigotimoteo.animally.presentation.repromedication

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.GetReproMedicationsByPatientUseCase
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
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReproMedicationListViewModelTest {
    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    private val getReproMedicationsByPatientUseCase = GetReproMedicationsByPatientUseCase(reproMedicationRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val reproMedication =
        ReproMedication(
            id = 1L,
            patientId = 1L,
            medication = "Regumate",
            dateAdministered = LocalDate(2026, 1, 15),
            dosage = "0.044 mg/kg",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ReproMedicationListViewModel(
            patientId = 1L,
            getReproMedicationsByPatientUseCase = getReproMedicationsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads reproduction medications on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val medications = listOf(reproMedication)
            every { reproMedicationRepositoryMock.getByPatient(1L) } returns medications
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(medications, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add reproduction medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditReproMed(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit reproduction medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.getByPatient(1L) } returns listOf(reproMedication)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(reproMedication.id)

            assertEquals(Route.AddEditReproMed(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { reproMedicationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
