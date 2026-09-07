@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantConversationGrouper
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantRecordSource
import com.github.rodrigotimoteo.animally.domain.assistant.model.conversationKey
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import com.github.rodrigotimoteo.animally.llm.EngineType
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantChatMessage
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantUiState
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/** Objective-C-friendly projection of one persisted assistant turn. */
@ObjCName("AssistantHistoryItem")
data class AssistantHistoryItem(
    val id: Long,
    val conversationId: String,
    val question: String,
    val answer: String,
    val source: String,
    val interrupted: Boolean,
    val createdAtMillis: Long,
    val webSources: List<VeterinaryWebSource> = emptyList(),
    val recordSources: List<AssistantHistorySource> = emptyList(),
)

/** Objective-C-friendly identity for a local record cited by a saved answer. */
@ObjCName("AssistantHistorySource")
data class AssistantHistorySource(
    val patientId: Long,
    val patientName: String,
    val recordType: String,
    val recordId: Long,
    val date: String? = null,
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
    val conversations: List<AssistantConversationItem> = emptyList(),
)

/** Swift-facing projection of one multi-turn assistant conversation. */
@ObjCName("AssistantConversationItem")
data class AssistantConversationItem(
    val id: String,
    val title: String,
    val preview: String,
    val turnCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val turns: List<AssistantHistoryItem>,
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
        ui.history.map(::toHistoryItem).let { history ->
            AssistantStoreState(
                availability = ui.availability,
                messages = ui.messages,
                isGenerating = ui.isGenerating,
                error = ui.error,
                isHistoryLoading = ui.isHistoryLoading,
                historyError = ui.historyError,
                history = history,
                conversations =
                    AssistantConversationGrouper
                        .group(ui.history)
                        .map { conversation ->
                            AssistantConversationItem(
                                id = conversation.id,
                                title = conversation.title,
                                preview = conversation.preview,
                                turnCount = conversation.turns.size,
                                createdAtMillis = conversation.createdAt.toEpochMilliseconds(),
                                updatedAtMillis = conversation.updatedAt.toEpochMilliseconds(),
                                turns = conversation.turns.map(::toHistoryItem),
                            )
                        },
            )
        }

    private fun toHistoryItem(turn: AssistantChatTurn): AssistantHistoryItem =
        AssistantHistoryItem(
            id = turn.id,
            conversationId = turn.conversationKey(),
            question = turn.question,
            answer = turn.answer,
            source = turn.source,
            interrupted = turn.interrupted,
            createdAtMillis = turn.createdAt.toEpochMilliseconds(),
            webSources = turn.webSources,
            recordSources = turn.recordSources.map(::toHistorySource),
        )

    private fun toHistorySource(source: AssistantRecordSource): AssistantHistorySource =
        AssistantHistorySource(
            patientId = source.patientId,
            patientName = source.patientName,
            recordType = source.recordType,
            recordId = source.recordId,
            date = source.date,
        )

    /** Asks the assistant a free-text question about the records. */
    fun ask(question: String) {
        viewModel.ask(question)
    }

    /** Stops an in-flight response and leaves its partial text retryable. */
    fun cancelGeneration() {
        viewModel.cancelGeneration()
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

    /** Cancels the ViewModel scope when the Swift screen is released. */
    fun clear() {
        viewModel.viewModelScope.cancel()
    }

    /** Convenience mirror of the availability value for one-shot checks. */
    val availability: LlmAvailability
        get() = viewModel.uiState.value.availability
}
