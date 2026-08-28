package com.github.rodrigotimoteo.animally.presentation.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.DeleteDictationCaptureUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.GetDictationCapturesUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.SaveDictationCaptureUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.UpdateDictationCaptureTranscriptUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.PatientResolution
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import com.github.rodrigotimoteo.animally.llm.GenerateDictationSessionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Review state of one dictated suggestion.
 *
 * @property record The validated suggestion.
 * @property resolution Outcome of resolving [record]'s patient name, or `null`
 *   when the suggestion carries no patient name or has not been resolved.
 * @property decision User decision: `true` accepted, `false` rejected,
 *   `null` pending.
 */
data class DictationSuggestionUi(
    val record: SuggestedRecord,
    val resolution: PatientResolution? = null,
    val decision: Boolean? = null,
)

/**
 * UI state of the dictation review screen.
 *
 * @property transcript Raw transcript text as captured from speech.
 * @property suggestions Validated suggestions awaiting accept/reject.
 * @property error Decode failure message, or `null` when the last session JSON parsed.
 * @property captures Previously completed dictations, newest first.
 * @property captureSearchQuery Text used to filter the dictation archive.
 * @property isCapturesLoading Whether the archive is reading from persistence.
 * @property captureError Persistence error for the archive, if any.
 */
data class DictationUiState(
    val transcript: String = "",
    val suggestions: List<DictationSuggestionUi> = emptyList(),
    val error: String? = null,
    val captures: List<DictationCapture> = emptyList(),
    val captureSearchQuery: String = "",
    val isCapturesLoading: Boolean = false,
    val captureError: String? = null,
) {
    /** Captures matching [captureSearchQuery], preserving newest-first order. */
    val filteredCaptures: List<DictationCapture>
        get() {
            val query = captureSearchQuery.trim()
            return if (query.isEmpty()) {
                captures
            } else {
                captures.filter { it.transcript.contains(query, ignoreCase = true) }
            }
        }
}

/**
 * View model for the voice-dictation review flow.
 *
 * Holds the transcript, decodes the dictated session JSON into validated
 * suggestions, resolves patient names, and tracks per-suggestion accept/reject
 * decisions taken by the user.
 *
 * @param validateSuggestionsUseCase Validates raw dictated records.
 * @param resolvePatientUseCase Resolves spoken patient names to patients.
 * @param getDictationCapturesUseCase Loads the local dictation archive.
 * @param saveDictationCaptureUseCase Persists a completed capture.
 * @param updateDictationCaptureTranscriptUseCase Persists reviewed transcript edits.
 * @param deleteDictationCaptureUseCase Removes a capture and its audio file.
 * @param ioDispatcher Dispatcher for database and file work.
 * @param generateDictationSession Cloud fallback for structured extraction on
 *   devices without Apple Intelligence.
 * @param isCloudReady True when the configured cloud route can accept a request.
 */
class DictationViewModel(
    private val validateSuggestionsUseCase: ValidateSuggestionsUseCase,
    private val resolvePatientUseCase: ResolvePatientUseCase,
    private val getDictationCapturesUseCase: GetDictationCapturesUseCase,
    private val saveDictationCaptureUseCase: SaveDictationCaptureUseCase,
    private val updateDictationCaptureTranscriptUseCase: UpdateDictationCaptureTranscriptUseCase,
    private val deleteDictationCaptureUseCase: DeleteDictationCaptureUseCase,
    private val ioDispatcher: CoroutineDispatcher,
    private val generateDictationSession: GenerateDictationSessionUseCase? = null,
    private val isCloudReady: () -> Boolean = { false },
) : ViewModel() {
    private val _uiState = MutableStateFlow(DictationUiState())
    private var captureOperation: Job? = null

    /** The current dictation review state. */
    val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    init {
        reloadCaptures()
    }

    /** Updates the raw transcript text. */
    fun setTranscript(value: String) {
        _uiState.update { it.copy(transcript = value) }
    }

    /** True when the iOS edge can use the configured cloud extractor. */
    fun canUseCloudExtraction(): Boolean = generateDictationSession != null && isCloudReady()

    /**
     * Extracts a transcript through the routed Kotlin LLM seam. Native Swift
     * only supplies speech text and the selected language; JSON decoding and
     * the cloud readiness policy stay in the shared layer.
     */
    suspend fun extract(
        transcript: String,
        language: String,
    ): String {
        val normalizedTranscript = transcript.trim()
        require(normalizedTranscript.isNotEmpty()) { "There is no transcript to extract." }
        val extractor = generateDictationSession
        check(extractor != null && isCloudReady()) {
            "Structured extraction needs Apple Intelligence or an enabled Cloud AI model. " +
                "Enable Cloud AI in Settings and try again."
        }
        return extractor(normalizedTranscript, language)
    }

    /** Updates the archive filter without touching the current review. */
    fun setCaptureSearchQuery(value: String) {
        _uiState.update { it.copy(captureSearchQuery = value) }
    }

    /** Refreshes the persisted dictation archive. */
    fun reloadCaptures() {
        runCaptureOperation {
            getDictationCapturesUseCase()
        }
    }

    /**
     * Saves the completed capture immediately after recording stops.
     *
     * A transcript is allowed to be blank when the audio file exists: this
     * keeps the original recording available for manual checking even when
     * speech recognition or structured extraction failed.
     */
    suspend fun saveCapture(
        transcript: String,
        audioPath: String?,
        durationMillis: Long?,
    ): Long? {
        val normalizedTranscript = transcript.trim()
        val normalizedAudioPath = audioPath?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedTranscript.isEmpty() && normalizedAudioPath == null) return null

        val (id, captures) =
            withContext(ioDispatcher) {
                val id =
                    saveDictationCaptureUseCase(
                        DictationCapture(
                            transcript = normalizedTranscript,
                            audioPath = normalizedAudioPath,
                            durationMillis = durationMillis?.takeIf { it >= 0L },
                            capturedAt = Clock.System.now(),
                        ),
                    )
                id to getDictationCapturesUseCase()
            }
        _uiState.update {
            it.copy(
                captures = captures,
                isCapturesLoading = false,
                captureError = null,
            )
        }
        return id
    }

    /** Saves the edited transcript while preserving the original recording. */
    suspend fun updateCaptureTranscript(
        id: Long,
        transcript: String,
    ): Boolean {
        val updated =
            withContext(ioDispatcher) {
                updateDictationCaptureTranscriptUseCase(id, transcript)
            }
        if (updated) {
            val captures = withContext(ioDispatcher) { getDictationCapturesUseCase() }
            _uiState.update {
                it.copy(
                    captures = captures,
                    isCapturesLoading = false,
                    captureError = null,
                )
            }
        }
        return updated
    }

    /** Deletes one archive entry and best-effort removes its audio artifact. */
    fun deleteCapture(id: Long) {
        runCaptureOperation {
            deleteDictationCaptureUseCase(id)
            getDictationCapturesUseCase()
        }
    }

    /**
     * Decodes [sessionJson], validates its records and resolves patient names.
     *
     * Replaces any previous suggestion list. On decode failure the error is
     * surfaced in the state and existing suggestions are kept untouched.
     *
     * @param sessionJson The dictated session JSON produced by the LLM.
     */
    fun validate(sessionJson: String) {
        val decoded = runCatching { json.decodeFromString<DictatedSessionDto>(sessionJson) }
        if (decoded.isFailure) {
            _uiState.update { it.copy(error = decoded.exceptionOrNull()?.message) }
            return
        }
        val validated =
            validateSuggestionsUseCase(decoded.getOrThrow().records)
                // Structurally invalid suggestions must not reach the review
                // list: they have no save path and would only offer a dead
                // accept/reject choice. Filtered here so the visible list and
                // the accept/reject indices stay aligned.
                .filter { it.validation !is SuggestedValidationState.Dropped }
        val suggestions =
            validated.map { record ->
                DictationSuggestionUi(
                    record = record,
                    resolution = record.patientName?.let { resolvePatientUseCase(it) },
                )
            }
        _uiState.update { it.copy(suggestions = suggestions, error = null) }
    }

    /** Marks the suggestion at [index] as accepted for insertion. */
    fun accept(index: Int) {
        updateSuggestionAt(index) { it.copy(decision = true) }
    }

    /** Marks the suggestion at [index] as rejected by the user. */
    fun reject(index: Int) {
        updateSuggestionAt(index) { it.copy(decision = false) }
    }

    private fun updateSuggestionAt(
        index: Int,
        transform: (DictationSuggestionUi) -> DictationSuggestionUi,
    ) {
        _uiState.update { state ->
            if (index !in state.suggestions.indices) return@update state
            state.copy(suggestions = state.suggestions.mapIndexed { i, s -> if (i == index) transform(s) else s })
        }
    }

    private fun runCaptureOperation(operation: suspend () -> List<DictationCapture>) {
        captureOperation?.cancel()
        captureOperation =
            viewModelScope.launch {
                _uiState.update { it.copy(isCapturesLoading = true, captureError = null) }
                try {
                    val captures = withContext(ioDispatcher) { operation() }
                    _uiState.update {
                        it.copy(
                            captures = captures,
                            isCapturesLoading = false,
                            captureError = null,
                        )
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    _uiState.update {
                        it.copy(
                            isCapturesLoading = false,
                            captureError = t.message ?: "Could not update dictation history.",
                        )
                    }
                }
            }
    }
}
