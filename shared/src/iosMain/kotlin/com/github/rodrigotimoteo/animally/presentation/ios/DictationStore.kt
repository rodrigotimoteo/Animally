@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.dictation.InsertSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.InsertionResult
import com.github.rodrigotimoteo.animally.domain.dictation.SuggestedInsertion
import com.github.rodrigotimoteo.animally.presentation.dictation.DictationSuggestionUi
import com.github.rodrigotimoteo.animally.presentation.dictation.DictationUiState
import com.github.rodrigotimoteo.animally.presentation.dictation.DictationViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the dictation review screen.
 *
 * @property transcript Raw transcript text as captured from speech.
 * @property suggestions Validated suggestions awaiting accept/reject.
 * @property error Decode failure message, or `null` when the last session JSON parsed.
 */
@ObjCName("DictationStoreState")
data class DictationStoreState(
    val transcript: String = "",
    val suggestions: List<DictationSuggestionUi> = emptyList(),
    val error: String? = null,
)

/**
 * Swift-facing store wrapping [DictationViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("DictationStore")
class DictationStore(
    private val viewModel: DictationViewModel,
    private val insertSuggestionsUseCase: InsertSuggestionsUseCase,
) {
    /** Observable state of the dictation review screen. */
    val state: NativeFlow<DictationStoreState> =
        NativeFlow(
            viewModel.uiState.map(::toStoreState).stateIn(
                scope = viewModel.viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DictationStoreState(),
            ),
            viewModel.viewModelScope,
        )

    private fun toStoreState(ui: DictationUiState): DictationStoreState =
        DictationStoreState(
            transcript = ui.transcript,
            suggestions = ui.suggestions,
            error = ui.error,
        )

    /** Updates the raw transcript text. */
    fun setTranscript(value: String) {
        viewModel.setTranscript(value)
    }

    /**
     * Decodes [sessionJson], validates its records and resolves patient names.
     */
    fun validate(sessionJson: String) {
        viewModel.validate(sessionJson)
    }

    /** Marks the suggestion at index as accepted for insertion. */
    fun accept(index: Long) {
        viewModel.accept(index.toInt())
    }

    /** Marks the suggestion at index as rejected by the user. */
    fun reject(index: Long) {
        viewModel.reject(index.toInt())
    }

    /**
     * Persists the suggestions currently marked as accepted.
     *
     * [patientIds] is ordered exactly like the accepted suggestions in the
     * current review state. The Swift review screen owns disambiguation, so
     * the bridge accepts the resolved ids explicitly instead of guessing from
     * a spoken name. A non-null return value describes a validation or partial
     * insertion failure; callers must keep the review visible in that case.
     */
    fun saveAccepted(patientIds: List<Long>): String? {
        val accepted =
            viewModel.uiState.value.suggestions
                .filter { it.decision == true }
        return when {
            accepted.isEmpty() -> "Select at least one record to save."
            accepted.size != patientIds.size || patientIds.any { it <= 0L } ->
                "Choose a patient for every accepted record before saving."
            else -> {
                val insertions =
                    accepted.mapIndexed { index, suggestion ->
                        SuggestedInsertion(
                            record = suggestion.record,
                            patientId = patientIds[index],
                            // The explicit Accept action is the user's acknowledgement
                            // for flagged-but-saveable suggestions.
                            acknowledgedFlags = true,
                        )
                    }
                val outcomes = insertSuggestionsUseCase(insertions)
                val failures = outcomes.filterIsInstance<InsertionResult.Failed>()
                if (failures.isEmpty()) {
                    null
                } else {
                    val insertedCount = outcomes.count { it is InsertionResult.Inserted }
                    val failureSummary = failures.joinToString("; ") { it.message }
                    if (insertedCount == 0) {
                        "No records were saved: $failureSummary"
                    } else {
                        "Saved $insertedCount record(s), but ${failures.size} could not be saved: $failureSummary"
                    }
                }
            }
        }
    }
}
