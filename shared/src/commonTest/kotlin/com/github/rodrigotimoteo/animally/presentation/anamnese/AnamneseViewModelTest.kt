package com.github.rodrigotimoteo.animally.presentation.anamnese

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.GetAnamneseByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.SaveAnamneseUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
            assertFalse(assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `save creates new anamnese and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns null
            every { anamneseRepositoryMock.save(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onGeneralHistoryChange("History")
            vm.onAllergiesChange("Hay")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                anamneseRepositoryMock.save(
                    matches {
                        it.id == 0L && it.patientId == 1L && it.generalHistory == "History" && it.allergies == "Hay"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
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

    @Test
    fun `onChronicConditionsChange updates chronicConditions`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns null
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onChronicConditionsChange("Recurrent colic")

            assertEquals("Recurrent colic", vm.formState.value?.chronicConditions)
        }

    @Test
    fun `load failure resets form to blank without error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertEquals(AnamneseFormState(), vm.formState.value)
        }

    @Test
    fun `save failure resets isSaving without error and does not navigate and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { anamneseRepositoryMock.getByPatient(1L) } returns null
            every { anamneseRepositoryMock.save(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onGeneralHistoryChange("History")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals(AnamneseFormState(generalHistory = "History"), form)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
