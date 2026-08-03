package com.github.rodrigotimoteo.animally.presentation.ownerDetail

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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
class OwnerDetailViewModelTest {
    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private val getOwnerDetailUseCase = GetOwnerDetailUseCase(ownerRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val owner =
        Owner(
            id = 1L,
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val patient =
        Patient(
            id = 10L,
            name = "Midnight",
            species = "Equine",
            breed = null,
            dateOfBirth = LocalDate(2020, 5, 1),
            ownerId = 1L,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        OwnerDetailViewModel(owner.id, getOwnerDetailUseCase, patientRepositoryMock, navigator, dispatcher)

    @Test
    fun `load populates owner and linked patients`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            every { patientRepositoryMock.getPatientsByOwnerId(owner.id) } returns listOf(patient)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(owner, vm.uiState.value.owner)
            assertEquals(listOf(patient), vm.uiState.value.patients)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `load with unknown owner keeps empty state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns null
            every { patientRepositoryMock.getPatientsByOwnerId(owner.id) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertNull(vm.uiState.value.owner)
            assertEquals(emptyList(), vm.uiState.value.patients)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on edit click navigates to add edit owner`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            every { patientRepositoryMock.getPatientsByOwnerId(owner.id) } returns listOf(patient)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick()

            assertEquals(Route.AddEditOwner(owner.id), navigator.backStack.last())
        }

    @Test
    fun `on back pops back stack`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } returns owner
            every { patientRepositoryMock.getPatientsByOwnerId(owner.id) } returns listOf(patient)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick()
            vm.onBack()

            assertEquals(Route.PatientList, navigator.backStack.last())
        }

    @Test
    fun `on dismiss error clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(owner.id) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
