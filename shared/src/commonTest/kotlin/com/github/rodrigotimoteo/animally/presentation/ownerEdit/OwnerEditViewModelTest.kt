package com.github.rodrigotimoteo.animally.presentation.ownerEdit

import app.cash.turbine.test
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.SaveOwnerUseCase
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerEditViewModelTest {
    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val getOwnerDetailUseCase = GetOwnerDetailUseCase(ownerRepositoryMock)

    private val saveOwnerUseCase = SaveOwnerUseCase(ownerRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val owner =
        Owner(
            id = 1L,
            name = "Bob",
            phone = "123",
            email = null,
            address = "Addr",
            createdAt = Instant.fromEpochMilliseconds(100L),
            updatedAt = Instant.fromEpochMilliseconds(100L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create mode with blank name sets nameError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = OwnerEditViewModel(null, getOwnerDetailUseCase, saveOwnerUseCase, navigator, StandardTestDispatcher(testScheduler))

            vm.formState.test {
                assertEquals(OwnerFormState(), awaitItem())

                vm.save()

                assertEquals(OwnerFormState(nameError = "Name is required"), awaitItem())
            }

            verify(VerifyMode.exactly(0)) { ownerRepositoryMock.insertOwner(any()) }
        }

    @Test
    fun `create mode with valid name inserts owner and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.insertOwner(any()) } returns 1L
            val vm = OwnerEditViewModel(null, getOwnerDetailUseCase, saveOwnerUseCase, navigator, StandardTestDispatcher(testScheduler))

            vm.onNameChange("Alice")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) { ownerRepositoryMock.insertOwner(any()) }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `edit mode prefills form from loaded owner`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(1L) } returns owner
            val vm = OwnerEditViewModel(1L, getOwnerDetailUseCase, saveOwnerUseCase, navigator, StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(OwnerFormState(id = 1L, name = "Bob", phone = "123", address = "Addr", createdAt = owner.createdAt), vm.formState.value)
            assertFalse(vm.formState.value!!.isLoading)
        }

    @Test
    fun `edit mode saves with loaded owner id`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { ownerRepositoryMock.getOwnerById(1L) } returns owner
            every { ownerRepositoryMock.updateOwner(any()) } returns 1L
            val vm = OwnerEditViewModel(1L, getOwnerDetailUseCase, saveOwnerUseCase, navigator, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onNameChange("Bob Updated")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                ownerRepositoryMock.updateOwner(matches { it.id == 1L && it.name == "Bob Updated" })
            }
            assertTrue(navigator.backStack.isEmpty())
        }
}
