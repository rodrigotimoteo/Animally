@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import com.github.rodrigotimoteo.animally.llm.EngineType
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantChatMessage
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantUiState
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/** Objective-C-friendly projection of one persisted assistant turn. */
@ObjCName("AssistantHistoryItem")
data class AssistantHistoryItem(
    val id: Long,
    val question: String,
    val answer: String,
    val source: String,
    val interrupted: Boolean,
    val createdAtMillis: Long,
)

/** Swift-facing assistant state with a primitive history projection. */
@ObjCName("AssistantStoreState")
data class AssistantStoreState(
    val availability: LlmAvailability = LlmAvailability.Loading(EngineType.FOUNDATION_MODELS),
    val messages: List<AssistantChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val isHistoryLoading: Boolean = true,
    val historyError: String? = null,
    val history: List<AssistantHistoryItem> = emptyList(),
)

/**
 * Swift-facing store wrapping [AssistantViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("AssistantStore")
class AssistantStore(
    private val viewModel: AssistantViewModel,
) {
    /** Observable state of the AI assistant screen. */
    val state: NativeFlow<AssistantStoreState> =
        NativeFlow(
            viewModel.uiState.map(::toStoreState).stateIn(
                scope = viewModel.viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AssistantStoreState(),
            ),
            viewModel.viewModelScope,
        )

    private fun toStoreState(ui: AssistantUiState): AssistantStoreState =
        AssistantStoreState(
            availability = ui.availability,
            messages = ui.messages,
            isGenerating = ui.isGenerating,
            error = ui.error,
            isHistoryLoading = ui.isHistoryLoading,
            historyError = ui.historyError,
            history = ui.history.map(::toHistoryItem),
        )

    private fun toHistoryItem(turn: AssistantChatTurn): AssistantHistoryItem =
        AssistantHistoryItem(
            id = turn.id,
            question = turn.question,
            answer = turn.answer,
            source = turn.source,
            interrupted = turn.interrupted,
            createdAtMillis = turn.createdAt.toEpochMilliseconds(),
        )

    /** Asks the assistant a free-text question about the records. */
    fun ask(question: String) {
        viewModel.ask(question)
    }

    /** Re-checks platform LLM availability. */
    fun refreshAvailability() {
        viewModel.refreshAvailability()
    }

    /** Reloads the persisted recent-turn list for the history screen. */
    fun refreshHistory() {
        viewModel.refreshHistory()
    }

    /** Starts a blank visible conversation while retaining persisted history. */
    fun startNewChat() {
        viewModel.startNewChat()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.dismissError()
    }

    /** Convenience mirror of the availability value for one-shot checks. */
    val availability: LlmAvailability
        get() = viewModel.uiState.value.availability
}
