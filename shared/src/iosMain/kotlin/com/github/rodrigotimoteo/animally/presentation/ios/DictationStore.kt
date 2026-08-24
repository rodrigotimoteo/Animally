@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
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
}
