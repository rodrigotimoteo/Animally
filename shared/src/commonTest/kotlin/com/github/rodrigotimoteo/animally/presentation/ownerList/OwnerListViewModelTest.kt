package com.github.rodrigotimoteo.animally.presentation.ownerList

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.DeleteOwnerUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerListViewModelTest {
    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private val getOwnerListUseCase = GetOwnerListUseCase(ownerRepositoryMock)

    private val searchRepository = FakeSearchRepository()

    private val deleteOwnerUseCase = DeleteOwnerUseCase(ownerRepositoryMock, patientRepositoryMock, searchRepository)

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

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) = OwnerListViewModel(getOwnerListUseCase, deleteOwnerUseCase, navigator, dispatcher)

    @Test
    fun `loads owners on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owners = listOf(owner)
            every { ownerRepositoryMock.getOwnerList() } returns owners
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(owners, vm.uiState.value.owners)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on owner click navigates to owner detail`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerList() } returns listOf(owner)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onOwnerClick(owner.id)

            assertEquals(Route.OwnerDetail(owner.id), navigator.backStack.last())
        }

    @Test
    fun `on add click navigates to add edit owner`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerList() } returns listOf(owner)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditOwner(), navigator.backStack.last())
        }

    @Test
    fun `delete success reloads owners`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerList() } returns listOf(owner)
            every { patientRepositoryMock.countPatientsByOwnerId(owner.id) } returns 0L
            every { ownerRepositoryMock.setInactive(owner.id, any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onDeleteClick(owner.id)
            advanceUntilIdle()

            verify(VerifyMode.exactly(2)) { ownerRepositoryMock.getOwnerList() }
            verify(VerifyMode.exactly(1)) { ownerRepositoryMock.setInactive(owner.id, any()) }
        }

    @Test
    fun `delete blocked sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerList() } returns listOf(owner)
            every { patientRepositoryMock.countPatientsByOwnerId(owner.id) } returns 2L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onDeleteClick(owner.id)
            advanceUntilIdle()

            assertEquals("Owner has 2 patients. Unassign first.", vm.uiState.value.errorMessage)
            verify(VerifyMode.exactly(0)) { ownerRepositoryMock.setInactive(owner.id, any()) }

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
