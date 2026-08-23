package com.github.rodrigotimoteo.animally.presentation.lameness

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.DeleteLamenessUseCase
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.GetLamenessListByPatientUseCase
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LamenessListViewModelTest {
    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private val getLamenessListByPatientUseCase = GetLamenessListByPatientUseCase(lamenessRepositoryMock)

    private val deleteLamenessUseCase = DeleteLamenessUseCase(lamenessRepositoryMock, FakeSearchRepository())

    private val navigator = AnimallyNavigator()

    private val lameness =
        Lameness(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            gradeAAEP = 3,
            diagnosis = "Suspensory desmitis",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        LamenessListViewModel(
            patientId = 1L,
            getLamenessListByPatientUseCase = getLamenessListByPatientUseCase,
            deleteLamenessUseCase = deleteLamenessUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads lameness records on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val records = listOf(lameness)
            every { lamenessRepositoryMock.getByPatient(1L) } returns records
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(records, vm.uiState.value.records)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add lameness`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { lamenessRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditLameness(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit lameness`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { lamenessRepositoryMock.getByPatient(1L) } returns listOf(lameness)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(lameness.id)

            assertEquals(Route.AddEditLameness(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { lamenessRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
