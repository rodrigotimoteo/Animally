package com.github.rodrigotimoteo.animally.presentation.consultation

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.GetConsultationsByPatientUseCase
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
class ConsultationListViewModelTest {
    private val consultationRepositoryMock: IConsultationRepository = mock()

    private val getConsultationsByPatientUseCase = GetConsultationsByPatientUseCase(consultationRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val consultation =
        Consultation(
            id = 1L,
            patientId = 1L,
            date = LocalDate(2026, 1, 15),
            subjective = "Owner reports lameness",
            objective = "",
            assessment = "",
            plan = "",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        ConsultationListViewModel(
            patientId = 1L,
            getConsultationsByPatientUseCase = getConsultationsByPatientUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `loads consultations on init`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val consultations = listOf(consultation)
            every { consultationRepositoryMock.getByPatient(1L) } returns consultations
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(consultations, vm.uiState.value.consultations)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `on add click navigates to add consultation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { consultationRepositoryMock.getByPatient(1L) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onAddClick()

            assertEquals(Route.AddEditConsultation(1L), navigator.backStack.last())
        }

    @Test
    fun `on edit click navigates to edit consultation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { consultationRepositoryMock.getByPatient(1L) } returns listOf(consultation)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEditClick(consultation.id)

            assertEquals(Route.AddEditConsultation(1L, 1L), navigator.backStack.last())
        }

    @Test
    fun `load failure sets error message and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { consultationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }
}
