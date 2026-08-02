package com.github.rodrigotimoteo.animally.presentation.customreminder

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.DeleteCustomReminderUseCase
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.GetCustomRemindersByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CustomReminderListViewModelTest {
    private val customReminderRepositoryMock: ICustomReminderRepository = mock()

    private val getCustomRemindersByPatientUseCase = GetCustomRemindersByPatientUseCase(customReminderRepositoryMock)

    private val deleteCustomReminderUseCase = DeleteCustomReminderUseCase(customReminderRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val upcoming =
        CustomReminder(
            id = 1L,
            patientId = 1L,
            title = "Vaccination booster",
            dueDate = LocalDate(2030, 1, 15),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val overdue =
        CustomReminder(
            id = 2L,
            patientId = 1L,
            title = "Farrier check",
            dueDate = LocalDate(2000, 1, 15),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        CustomReminderListViewModel(
            patientId = 1L,
            getCustomRemindersByPatientUseCase = getCustomRemindersByPatientUseCase,
            deleteCustomReminderUseCase = deleteCustomReminderUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads reminders split into upcoming and overdue groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.getByPatient(1L) } returns listOf(overdue, upcoming)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(listOf(upcoming), vm.uiState.value.upcoming)
            assertEquals(listOf(overdue), vm.uiState.value.overdue)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add custom reminder`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditCustomReminder(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit custom reminder`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.getByPatient(1L) } returns listOf(upcoming)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(upcoming.id)

            assertEquals(Route.AddEditCustomReminder(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `on delete click deactivates reminder and reloads list`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.getByPatient(1L) } returns listOf(overdue, upcoming)
            every { customReminderRepositoryMock.setInactive(1L, any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            every { customReminderRepositoryMock.getByPatient(1L) } returns listOf(overdue)
            vm.onDeleteClick(upcoming.id)
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(1L, any()) }
            assertEquals(emptyList(), vm.uiState.value.upcoming)
            assertEquals(listOf(overdue), vm.uiState.value.overdue)
        }

    @Test
    fun `load failure surfaces error and dismiss clears it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { customReminderRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
