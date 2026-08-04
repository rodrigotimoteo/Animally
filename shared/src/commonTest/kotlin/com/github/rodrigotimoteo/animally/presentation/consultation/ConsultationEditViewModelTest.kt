package com.github.rodrigotimoteo.animally.presentation.consultation

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.GetConsultationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.SaveConsultationUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import dev.mokkery.MockMode
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
class ConsultationEditViewModelTest {
    private val consultationRepositoryMock: IConsultationRepository = mock()

    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private val getConsultationDetailUseCase = GetConsultationDetailUseCase(consultationRepositoryMock)

    private val saveConsultationUseCase = SaveConsultationUseCase(consultationRepositoryMock, searchRepositoryMock)

    private val navigator = AnimallyNavigator()

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        ConsultationEditViewModel(
            patientId = 1L,
            consultationId = null,
            getConsultationDetailUseCase = getConsultationDetailUseCase,
            saveConsultationUseCase = saveConsultationUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `blank date sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("")
            vm.save()

            assertEquals("Date is required", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { consultationRepositoryMock.insert(any()) }
        }

    @Test
    fun `invalid date format sets dateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("15-01-2026")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.dateError)
            verify(VerifyMode.exactly(0)) { consultationRepositoryMock.insert(any()) }
        }

    @Test
    fun `valid form saves consultation with parsed date and emits Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { consultationRepositoryMock.insert(any()) } returns 1L
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onSubjectiveChange("Owner reports lameness")
            val receivedEffects = ArrayList<EditEffect>()
            val effectsJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.effects.collect { receivedEffects += it }
                }
            vm.save()
            advanceUntilIdle()

            verify(VerifyMode.exactly(1)) {
                consultationRepositoryMock.insert(
                    matches {
                        it.id == 0L &&
                            it.patientId == 1L &&
                            it.date == LocalDate(2026, 1, 15) &&
                            it.subjective == "Owner reports lameness"
                    },
                )
            }
            assertEquals(listOf(EditEffect.Saved), receivedEffects.toList())
            effectsJob.cancel()
        }

    @Test
    fun `invalid next visit date sets nextVisitDateError and does not save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onDateChange("2026-01-15")
            vm.onNextVisitDateChange("not-a-date")
            vm.save()

            assertEquals("Invalid date (YYYY-MM-DD)", vm.formState.value?.nextVisitDateError)
            verify(VerifyMode.exactly(0)) { consultationRepositoryMock.insert(any()) }
        }

    @Test
    fun `edit mode prefills form from loaded consultation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val consultation =
                Consultation(
                    id = 1L,
                    patientId = 1L,
                    date = LocalDate(2026, 1, 15),
                    subjective = "Owner reports lameness",
                    objective = "Left hind lame",
                    assessment = "Suspensory desmitis",
                    plan = "Rest and anti-inflammatory",
                    vetName = "Dr. X",
                    nextVisitDate = LocalDate(2026, 2, 15),
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                )
            every { consultationRepositoryMock.getById(1L) } returns consultation
            val vm =
                ConsultationEditViewModel(
                    patientId = 1L,
                    consultationId = 1L,
                    getConsultationDetailUseCase = getConsultationDetailUseCase,
                    saveConsultationUseCase = saveConsultationUseCase,
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            advanceUntilIdle()

            assertEquals(
                ConsultationFormState(
                    id = 1L,
                    date = "2026-01-15",
                    subjective = "Owner reports lameness",
                    objective = "Left hind lame",
                    assessment = "Suspensory desmitis",
                    plan = "Rest and anti-inflammatory",
                    vetName = "Dr. X",
                    nextVisitDate = "2026-02-15",
                    createdAt = consultation.createdAt,
                ),
                vm.formState.value,
            )
            assertTrue(!assertNotNull(vm.formState.value).isLoading)
        }

    @Test
    fun `SOAP note setters update objective assessment and plan`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onObjectiveChange("Left hind lame")
            vm.onAssessmentChange("Suspensory desmitis")
            vm.onPlanChange("Rest and NSAIDs")

            val form = assertNotNull(vm.formState.value)
            assertEquals("Left hind lame", form.objective)
            assertEquals("Suspensory desmitis", form.assessment)
            assertEquals("Rest and NSAIDs", form.plan)
        }

    @Test
    fun `onVetNameChange stores value and nulls when blank`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onVetNameChange("Dr. X")
            assertEquals("Dr. X", vm.formState.value?.vetName)

            vm.onVetNameChange("")
            assertEquals(null, vm.formState.value?.vetName)
        }

    @Test
    fun `save failure resets isSaving and sets dateError and emits no Saved effect`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { consultationRepositoryMock.insert(any()) } throws RuntimeException("db down")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

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
            assertEquals("db down", form.dateError)
            assertEquals(emptyList(), receivedEffects.toList())
            effectsJob.cancel()
        }
}
