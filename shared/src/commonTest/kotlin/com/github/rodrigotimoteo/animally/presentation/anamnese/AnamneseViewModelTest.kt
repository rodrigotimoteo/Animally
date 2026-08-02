package com.github.rodrigotimoteo.animally.presentation.anamnese

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.GetAnamneseByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.SaveAnamneseUseCase
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
class AnamneseViewModelTest {
    private val anamneseRepositoryMock: IAnamneseRepository = mock()

    private val getAnamneseByPatientUseCase = GetAnamneseByPatientUseCase(anamneseRepositoryMock)

    private val saveAnamneseUseCase = SaveAnamneseUseCase(anamneseRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val existingAnamnese =
        Anamnese(
            id = 1L,
            patientId = 1L,
            generalHistory = "Old history",
            chronicConditions = "Mild colic",
            allergies = "Penicillin",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        AnamneseViewModel(
            patientId = 1L,
            anamneseId = null,
            getAnamneseByPatientUseCase = getAnamneseByPatientUseCase,
            saveAnamneseUseCase = saveAnamneseUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `prefills form from existing record`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns existingAnamnese
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(
                AnamneseFormState(
                    id = 1L,
                    generalHistory = "Old history",
                    chronicConditions = "Mild colic",
                    allergies = "Penicillin",
                    createdAt = existingAnamnese.createdAt,
                ),
                vm.formState.value,
            )
            assertFalse(vm.formState.value!!.isLoading)
        }

    @Test
    fun `save creates new anamnese and navigates back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns null
            every { anamneseRepositoryMock.save(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onGeneralHistoryChange("History")
            vm.onAllergiesChange("Hay")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                anamneseRepositoryMock.save(
                    matches {
                        it.id == 0L && it.patientId == 1L && it.generalHistory == "History" && it.allergies == "Hay"
                    },
                )
            }
            assertTrue(navigator.backStack.isEmpty())
        }

    @Test
    fun `save updates existing record with loaded id`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns existingAnamnese
            every { anamneseRepositoryMock.save(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onGeneralHistoryChange("Updated history")
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                anamneseRepositoryMock.save(matches { it.id == 1L && it.generalHistory == "Updated history" })
            }
        }
}
