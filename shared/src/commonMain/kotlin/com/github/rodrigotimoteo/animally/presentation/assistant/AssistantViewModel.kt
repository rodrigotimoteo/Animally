package com.github.rodrigotimoteo.animally.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.llm.AssistantStrings
import com.github.rodrigotimoteo.animally.llm.GenerateRagResponseUseCase
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.llm.LlmEngine
import com.github.rodrigotimoteo.animally.llm.RagHistoryEntry
import com.github.rodrigotimoteo.animally.llm.RagStreamEvent
import com.github.rodrigotimoteo.animally.llm.assistantStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One turn of the assistant conversation.
 *
 * @property interrupted True when the generation stream failed mid-emission;
 *   [text] carries the partial reply and the UI offers a retry.
 * @property sources Retrieved records cited in [text] (source-card chips).
 * @property followUps Deterministic follow-up suggestions for this answer.
 */
data class AssistantChatMessage(
    val role: AssistantChatMessageRole,
    val text: String,
    val interrupted: Boolean = false,
    val sources: List<SearchResult> = emptyList(),
    val followUps: List<String> = emptyList(),
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

/** Transform applied to one chat message when updating state immutably. */
private typealias MessageTransform = (AssistantChatMessage) -> AssistantChatMessage

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
            var reply = ""
            try {
                generateRagResponse(trimmed, history).collect { event ->
                    if (event is RagStreamEvent.Chunk) reply = event.text
                    _uiState.update { current -> applyEvent(current, event, strings) }
                }
                if (reply.isBlank()) {
                    _uiState.update { current -> applyBlank(current, strings.blankReplyFallback) }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Exception) {
                // Engine failures normally arrive as Interrupted events; this
                // path covers anything thrown outside the stream. A blank
                // reply must never render as an empty bubble.
                _uiState.update { current ->
                    val text = reply.ifBlank { strings.blankReplyFallback }
                    val message = t.message ?: strings.blankReplyFallback
                    val patched = current.messages.upsertLast { it.copy(text = text) }
                    current.copy(messages = patched, error = message)
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

    private fun applyEvent(
        state: AssistantUiState,
        event: RagStreamEvent,
        i18n: AssistantStrings,
    ): AssistantUiState {
        val patched =
            when (event) {
                is RagStreamEvent.Chunk -> state.messages.upsertLast { it.copy(text = event.text) }
                is RagStreamEvent.Sources -> {
                    val citedTypes = event.sources.map(SearchResult::recordType)
                    state.messages.upsertLast { message ->
                        message.copy(
                            sources = event.sources,
                            followUps = FollowUpSuggestions.forCitations(citedTypes, i18n),
                        )
                    }
                }
                is RagStreamEvent.Interrupted ->
                    state.messages.upsertLast { it.copy(text = event.partialText, interrupted = true) }
            }
        return state.copy(messages = patched)
    }

    private fun applyBlank(
        state: AssistantUiState,
        text: String,
    ): AssistantUiState {
        val patched = state.messages.upsertLast { it.copy(text = text) }
        return state.copy(messages = patched)
    }

    /**
     * Replaces the trailing assistant turn with [transform]'s result, or
     * appends a fresh assistant turn when the transcript ends with anything
     * else (first reply of the conversation).
     */
    private fun List<AssistantChatMessage>.upsertLast(transform: MessageTransform): List<AssistantChatMessage> {
        val replacement =
            when (lastOrNull()?.role) {
                AssistantChatMessageRole.ASSISTANT -> dropLast(1) + transform(last())
                else -> this + transform(AssistantChatMessage(AssistantChatMessageRole.ASSISTANT, ""))
            }
        return replacement
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
