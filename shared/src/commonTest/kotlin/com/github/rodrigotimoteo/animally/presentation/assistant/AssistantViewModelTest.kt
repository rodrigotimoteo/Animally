package com.github.rodrigotimoteo.animally.presentation.assistant

import com.github.rodrigotimoteo.animally.llm.EnAssistantStrings
import com.github.rodrigotimoteo.animally.llm.RagHistoryEntry
import com.github.rodrigotimoteo.animally.llm.cloud.EngineSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantViewModelTest {
    @Test
    fun `interrupted replies are retained for review but excluded from prompt history`() {
        val messages =
            listOf(
                user("What happened to Bella?"),
                assistant("Bella had a partial answer", interrupted = true),
                user("What happened to Orion?"),
                assistant("Orion had a recorded vaccination."),
                user("This answer is still being generated"),
                assistant("", interrupted = false),
            )

        assertEquals(
            listOf(RagHistoryEntry("What happened to Orion?", "Orion had a recorded vaccination.")),
            assistantRagHistory(messages),
        )
    }

    @Test
    fun `completed replies remain in prompt history in transcript order`() {
        val messages =
            listOf(
                user("First question"),
                assistant("First answer"),
                user("Second question"),
                assistant("Second answer"),
            )

        assertEquals(
            listOf(
                RagHistoryEntry("First question", "First answer"),
                RagHistoryEntry("Second question", "Second answer"),
            ),
            assistantRagHistory(messages),
        )
    }

    @Test
    fun `unexpected generation failure marks the bubble retryable`() {
        val state =
            AssistantUiState(
                messages =
                    listOf(
                        user("Which patients do I have?"),
                        assistant(EnAssistantStrings.searchingPlaceholder),
                    ),
            )

        val failed =
            applyAssistantFailure(
                state = state,
                reply = "",
                error = IllegalStateException("provider unavailable"),
                strings = EnAssistantStrings,
                source = EngineSource.CLOUD,
            )

        val reply = failed.messages.last()
        assertTrue(reply.interrupted)
        assertEquals(EnAssistantStrings.blankReplyFallback, reply.text)
        assertEquals(EngineSource.CLOUD, reply.source)
        assertEquals("provider unavailable", failed.error)
    }

    private fun user(text: String): AssistantChatMessage =
        AssistantChatMessage(
            role = AssistantChatMessageRole.USER,
            text = text,
        )

    private fun assistant(
        text: String,
        interrupted: Boolean = false,
    ): AssistantChatMessage =
        AssistantChatMessage(
            role = AssistantChatMessageRole.ASSISTANT,
            text = text,
            interrupted = interrupted,
        )
}
