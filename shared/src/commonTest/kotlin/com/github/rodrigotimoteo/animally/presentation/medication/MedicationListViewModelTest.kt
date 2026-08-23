package com.github.rodrigotimoteo.animally.presentation.medication

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.medication.usecase.DeleteMedicationUseCase
import com.github.rodrigotimoteo.animally.domain.medication.usecase.GetMedicationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationListViewModelTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()
    private val searchRepositoryFake: FakeSearchRepository = FakeSearchRepository()

    private val getMedicationsByPatientUseCase = GetMedicationsByPatientUseCase(medicationRepositoryMock)

    private val deleteMedicationUseCase = DeleteMedicationUseCase(medicationRepositoryMock, searchRepositoryFake)

    private val navigator = AnimallyNavigator()

    private val medication =
        Medication(
            id = 1L,
            patientId = 1L,
            name = "Phenylbutazone",
            dosage = "2g",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        MedicationListViewModel(
            patientId = 1L,
            getMedicationsByPatientUseCase = getMedicationsByPatientUseCase,
            deleteMedicationUseCase = deleteMedicationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads medications on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(medication)
            every { medicationRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { medicationRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditMedication(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit medication`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { medicationRepositoryMock.getByPatient(1L) } returns listOf(medication)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(medication.id)

            assertEquals(Route.AddEditMedication(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { medicationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
