package com.github.rodrigotimoteo.animally.presentation.dictation

import androidx.lifecycle.ViewModel
import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import com.github.rodrigotimoteo.animally.domain.patient.usecase.PatientResolution
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

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
 */
data class DictationUiState(
    val transcript: String = "",
    val suggestions: List<DictationSuggestionUi> = emptyList(),
    val error: String? = null,
)

/**
 * View model for the voice-dictation review flow.
 *
 * Holds the transcript, decodes the dictated session JSON into validated
 * suggestions, resolves patient names, and tracks per-suggestion accept/reject
 * decisions taken by the user.
 *
 * @param validateSuggestionsUseCase Validates raw dictated records.
 * @param resolvePatientUseCase Resolves spoken patient names to patients.
 */
class DictationViewModel(
    private val validateSuggestionsUseCase: ValidateSuggestionsUseCase,
    private val resolvePatientUseCase: ResolvePatientUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DictationUiState())

    /** The current dictation review state. */
    val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /** Updates the raw transcript text. */
    fun setTranscript(value: String) {
        _uiState.update { it.copy(transcript = value) }
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
}
