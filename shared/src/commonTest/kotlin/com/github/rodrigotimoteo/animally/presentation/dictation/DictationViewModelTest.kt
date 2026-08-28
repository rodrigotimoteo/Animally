package com.github.rodrigotimoteo.animally.presentation.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.DeleteDictationCaptureUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.GetDictationCapturesUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.SaveDictationCaptureUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.UpdateDictationCaptureTranscriptUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import com.github.rodrigotimoteo.animally.llm.GenerateDictationSessionUseCase
import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock(MockMode.autoUnit)
    private val dictationCaptureRepositoryMock: IDictationCaptureRepository = mock(MockMode.autoUnit)

    init {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        every { dictationCaptureRepositoryMock.getAll() } returns emptyList()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        generateDictationSession: GenerateDictationSessionUseCase? = null,
        isCloudReady: () -> Boolean = { false },
    ): DictationViewModel =
        DictationViewModel(
            validateSuggestionsUseCase = ValidateSuggestionsUseCase(),
            resolvePatientUseCase = ResolvePatientUseCase(patientRepositoryMock),
            getDictationCapturesUseCase = GetDictationCapturesUseCase(dictationCaptureRepositoryMock),
            saveDictationCaptureUseCase = SaveDictationCaptureUseCase(dictationCaptureRepositoryMock),
            updateDictationCaptureTranscriptUseCase = UpdateDictationCaptureTranscriptUseCase(dictationCaptureRepositoryMock),
            deleteDictationCaptureUseCase = DeleteDictationCaptureUseCase(dictationCaptureRepositoryMock),
            ioDispatcher = Dispatchers.Unconfined,
            generateDictationSession = generateDictationSession,
            isCloudReady = isCloudReady,
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

    @Test
    fun `given configured cloud extraction then transcript is delegated to shared use case`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val engine =
                object : RagLlmEngine {
                    override fun generate(
                        prompt: String,
                        instructions: String,
                    ): Flow<String> =
                        flowOf(
                            """{"records":[{"recordType":"weight","patientName":"Trovao","weightKg":512.0}]}""",
                        )
                }
            val vm =
                createViewModel(
                    generateDictationSession = GenerateDictationSessionUseCase(engine),
                    isCloudReady = { true },
                )

            val sessionJson = vm.extract("Trovao weighed 512 kilos", "english")

            assertTrue(sessionJson.contains("\"recordType\":\"weight\""))
            assertTrue(sessionJson.contains("\"weightKg\":512.0"))
        }

    @Test
    fun `given cloud extraction is not ready then no model request is made`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(isCloudReady = { false })

            assertFailsWith<IllegalStateException> {
                vm.extract("Trovao weighed 512 kilos", "english")
            }
        }
}
