package com.github.rodrigotimoteo.animally.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.llm.AssistantStrings
import com.github.rodrigotimoteo.animally.llm.GenerateRagResponseUseCase
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.llm.LlmEngine
import com.github.rodrigotimoteo.animally.llm.RagHistoryEntry
import com.github.rodrigotimoteo.animally.llm.assistantStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One turn of the assistant conversation. */
data class AssistantChatMessage(
    val role: AssistantChatMessageRole,
    val text: String,
)

/** Author of an [AssistantChatMessage]. */
@kotlinx.serialization.Serializable
enum class AssistantChatMessageRole {
    USER,
    ASSISTANT,
}

/** Observable state of the AI assistant screen. */
data class AssistantUiState(
    val availability: LlmAvailability =
        LlmAvailability.Loading(
            com.github.rodrigotimoteo.animally.llm.EngineType.FOUNDATION_MODELS,
        ),
    val messages: List<AssistantChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel behind the AI assistant screen. Answers free-text questions about patient
 * records via retrieval-augmented generation ([GenerateRagResponseUseCase]) on top of
 * the platform [LlmEngine].
 *
 * Constructed manually by the iOS bridge (not via Koin annotations) following the
 * established store pattern.
 */
class AssistantViewModel(
    private val generateRagResponse: GenerateRagResponseUseCase,
    private val llmEngine: LlmEngine,
    private val strings: AssistantStrings = assistantStrings(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())

    /** Observable state of the assistant screen. */
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        refreshAvailability()
    }

    /** Re-checks platform LLM availability (call after settings changes or app resume). */
    fun refreshAvailability() {
        viewModelScope.launch {
            _uiState.update { it.copy(availability = llmEngine.availability()) }
        }
    }

    /**
     * Asks [question] against the record corpus. Appends the user turn immediately and
     * streams the assistant reply into state as it arrives. Completed prior turns are
     * passed as conversation history so follow-ups ("How old is she?") keep context.
     */
    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating) return

        val history = _uiState.value.messages.toRagHistory()
        _uiState.update {
            it.copy(
                messages = it.messages + AssistantChatMessage(AssistantChatMessageRole.USER, trimmed),
                isGenerating = true,
                error = null,
            )
        }

        viewModelScope.launch {
            // Each emission is the CUMULATIVE response text (streaming engines emit
            // full-so-far snapshots; non-streaming engines emit one final chunk), so
            // the last assistant turn is replaced in place — no append.
            var reply = ""
            try {
                generateRagResponse(trimmed, history).collect { text ->
                    reply = text
                    _uiState.update { state ->
                        state.copy(messages = state.messages.withAssistantTurn(text))
                    }
                }
                if (reply.isBlank()) {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.withAssistantTurn(strings.blankReplyFallback))
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages.withAssistantTurn(reply),
                        error = t.message ?: strings.blankReplyFallback,
                    )
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    /** Clears the current error message. */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun List<AssistantChatMessage>.withAssistantTurn(text: String): List<AssistantChatMessage> =
        when (lastOrNull()?.role) {
            AssistantChatMessageRole.ASSISTANT ->
                dropLast(1) + AssistantChatMessage(AssistantChatMessageRole.ASSISTANT, text)
            else -> this + AssistantChatMessage(AssistantChatMessageRole.ASSISTANT, text)
        }

    /**
     * Collapses the transcript into prior Q/A pairs for the RAG history:
     * each USER message pairs with the ASSISTANT reply that follows it. Pairs
     * whose reply never materialized (blank) are skipped - they carry no
     * context and would only dilute the prompt.
     */
    private fun List<AssistantChatMessage>.toRagHistory(): List<RagHistoryEntry> {
        val entries = mutableListOf<RagHistoryEntry>()
        var index = 0
        while (index < lastIndex) {
            val current = this[index]
            val next = this[index + 1]
            if (current.role == AssistantChatMessageRole.USER &&
                next.role == AssistantChatMessageRole.ASSISTANT &&
                next.text.isNotBlank()
            ) {
                entries.add(RagHistoryEntry(current.text, next.text))
                index += 2
            } else {
                index += 1
            }
        }
        return entries
    }
}
