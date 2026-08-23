package com.github.rodrigotimoteo.animally.presentation.substance

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.substance.usecase.GetControlledSubstanceDetailUseCase
import com.github.rodrigotimoteo.animally.domain.substance.usecase.SaveControlledSubstanceUseCase
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ControlledSubstanceEditViewModelTest {
    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private val getControlledSubstanceDetailUseCase = GetControlledSubstanceDetailUseCase(substanceRepositoryMock)

    private val saveControlledSubstanceUseCase = SaveControlledSubstanceUseCase(substanceRepositoryMock, FakeSearchRepository())

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        ControlledSubstanceEditViewModel(
            patientId = 1L,
            substanceId = null,
            getControlledSubstanceDetailUseCase = getControlledSubstanceDetailUseCase,
            saveControlledSubstanceUseCase = saveControlledSubstanceUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank drug name sets drugNameError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDoseChange("50")
            vm.onDateChange("2026-01-15")
            vm.save()

            assertEquals("Drug name is required", vm.formState.value?.drugNameError)
            verify(VerifyMode.exactly(0)) { substanceRepositoryMock.insert(any()) }
        }

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDrugNameChange("Xylazine")
            vm.onDoseChange("50")
            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { substanceRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves substance with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { substanceRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDrugNameChange("Xylazine")
            vm.onDoseChange("50")
            vm.onUnitChange("mg")
            vm.onDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                substanceRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.drugName == "Xylazine" &&
                            it.dose == "50" &&
                            it.unit == "mg" &&
                            it.date == LocalDate(2026, 1, 15)
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `edit mode prefills form from loaded substance`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val substance =
                ControlledSubstance(
                    id = 1L,
                    patientId = 1L,
                    drugName = "Xylazine",
                    dose = "50",
                    unit = "mg",
                    route = "IV",
                    administeredBy = "Dr. X",
                    witness = "Dr. Y",
                    date = LocalDate(2026, 1, 15),
                    reason = "Sedation",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { substanceRepositoryMock.getById(1L) } returns substance
            val vm =
                ControlledSubstanceEditViewModel(
                    patientId = 1L,
                    substanceId = 1L,
                    getControlledSubstanceDetailUseCase = getControlledSubstanceDetailUseCase,
                    saveControlledSubstanceUseCase = saveControlledSubstanceUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                ControlledSubstanceFormState(
                    id = 1L,
                    drugName = "Xylazine",
                    dose = "50",
                    unit = "mg",
                    route = "IV",
                    administeredBy = "Dr. X",
                    witness = "Dr. Y",
                    date = "2026-01-15",
                    reason = "Sedation",
                    createdAt = substance.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `optional field setters store values and null out on blank`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onRouteChange("IV")
            vm.onAdministeredByChange("Dr. X")
            vm.onWitnessChange("Dr. Y")
            vm.onReasonChange("Sedation")
            vm.onNotesChange("Observe after dose")

            val form = assertNotNull(vm.formState.value)
            assertEquals("IV", form.route)
            assertEquals("Dr. X", form.administeredBy)
            assertEquals("Dr. Y", form.witness)
            assertEquals("Sedation", form.reason)
            assertEquals("Observe after dose", form.notes)

            vm.onRouteChange("")
            vm.onAdministeredByChange("")
            vm.onWitnessChange("")
            vm.onReasonChange("")
            vm.onNotesChange("")

            val cleared = assertNotNull(vm.formState.value)
            assertEquals(null, cleared.route)
            assertEquals(null, cleared.administeredBy)
            assertEquals(null, cleared.witness)
            assertEquals(null, cleared.reason)
            assertEquals(null, cleared.notes)
        }

    @Test
    fun `blank dose sets doseError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDrugNameChange("Xylazine")
            vm.onDateChange("2026-01-15")
            vm.save()

            assertEquals("Dose is required", vm.formState.value?.doseError)
            verify(VerifyMode.exactly(0)) { substanceRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDrugNameChange("Xylazine")
            vm.onDoseChange("50")
            vm.onDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { substanceRepositoryMock.insert(any()) }
        }

    @Test
    fun `save failure resets isSaving and sets drugNameError and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { substanceRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDrugNameChange("Xylazine")
            vm.onDoseChange("50")
            vm.onDateChange("2026-01-15")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            val form = assertNotNull(vm.formState.value)
            assertFalse(form.isSaving)
            assertEquals("db down", form.drugNameError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
