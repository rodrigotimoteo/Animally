package com.github.rodrigotimoteo.animally.presentation.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock(MockMode.autoUnit)

    init {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DictationViewModel =
        DictationViewModel(
            validateSuggestionsUseCase = ValidateSuggestionsUseCase(),
            resolvePatientUseCase = ResolvePatientUseCase(patientRepositoryMock),
        )

    /**
     * One structurally valid weight suggestion plus one payload-less
     * suggestion (date only) that the validator drops.
     */
    private val sessionJson =
        """
        {
          "records": [
            {"recordType": "weight", "patientName": "Trovao", "date": "2026-08-20", "weightKg": 512.0},
            {"recordType": "ultrasound", "patientName": "Trovao", "date": "2026-08-20"}
          ]
        }
        """.trimIndent()

    @Test
    fun `given session with dropped suggestion when validated then dropped filtered from visible list`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel()

            vm.validate(sessionJson)

            val suggestions = vm.uiState.value.suggestions
            assertEquals(1, suggestions.size, "payload-less suggestion must not reach the review list")
            assertEquals(512.0, suggestions.single().record.weightKg)
            assertTrue(suggestions.single().record.validation !is SuggestedValidationState.Dropped)
        }

    @Test
    fun `given all-dropped session when validated then visible list empty without error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel()

            vm.validate("""{"records": [{"recordType": "weight", "date": "2026-08-20"}]}""")

            val state = vm.uiState.value
            assertTrue(state.suggestions.isEmpty())
            assertEquals(null, state.error)
        }
}
