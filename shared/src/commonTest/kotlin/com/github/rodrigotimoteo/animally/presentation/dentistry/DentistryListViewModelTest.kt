package com.github.rodrigotimoteo.animally.presentation.dentistry

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.GetDentistryListByPatientUseCase
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
class DentistryListViewModelTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private val getDentistryListByPatientUseCase = GetDentistryListByPatientUseCase(dentistryRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val record =
        Dentistry(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            findings = "Mild tartar",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        DentistryListViewModel(
            patientId = 1L,
            getDentistryListByPatientUseCase = getDentistryListByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads dentistry records on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(record)
            every { dentistryRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add dentistry`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditDentistry(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit dentistry`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.getByPatient(1L) } returns listOf(record)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(record.id)

            assertEquals(Route.AddEditDentistry(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure surfaces error and dismiss clears it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { dentistryRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
