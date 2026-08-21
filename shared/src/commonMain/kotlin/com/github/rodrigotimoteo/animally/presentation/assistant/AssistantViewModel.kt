package com.github.rodrigotimoteo.animally.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.llm.GenerateRagResponseUseCase
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.llm.LlmEngine
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
     * streams the assistant reply into state as it arrives.
     */
    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating) return

        _uiState.update {
            it.copy(
                messages = it.messages + AssistantChatMessage(AssistantChatMessageRole.USER, trimmed),
                isGenerating = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val reply = StringBuilder()
            try {
                generateRagResponse(trimmed).collect { chunk ->
                    reply.append(chunk)
                    _uiState.update { state ->
                        state.copy(messages = state.messages.withAssistantTurn(reply.toString()))
                    }
                }
                if (reply.isBlank()) {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.withAssistantTurn(FALLBACK_REPLY))
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages.withAssistantTurn(reply.toString()),
                        error = t.message ?: "The assistant could not answer right now.",
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

    private companion object {
        const val FALLBACK_REPLY = "I could not find anything relevant in the records."
    }
}
