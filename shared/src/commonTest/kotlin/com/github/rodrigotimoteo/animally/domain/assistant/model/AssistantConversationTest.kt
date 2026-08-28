package com.github.rodrigotimoteo.animally.domain.assistant.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AssistantConversationTest {
    @Test
    fun `turns from one session are grouped into one chronological block`() {
        val turns =
            listOf(
                turn(id = 2L, conversationId = "visit-a", question = "Follow-up", createdAt = 2_000L),
                turn(id = 1L, conversationId = "visit-a", question = "How is Luna?", createdAt = 1_000L),
                turn(id = 3L, conversationId = "visit-b", question = "Which patients?", createdAt = 3_000L),
            )

        val conversations = AssistantConversationGrouper.group(turns)

        assertEquals(2, conversations.size)
        assertEquals("visit-b", conversations[0].id)
        assertEquals("visit-a", conversations[1].id)
        assertEquals(listOf("How is Luna?", "Follow-up"), conversations[1].turns.map { it.question })
        assertEquals("How is Luna?", conversations[1].title)
        assertEquals("Answer to Follow-up", conversations[1].preview)
    }

    @Test
    fun `legacy rows get separate conversation blocks`() {
        val conversations =
            AssistantConversationGrouper.group(
                listOf(
                    turn(id = 11L, conversationId = "", question = "First", createdAt = 1_000L),
                    turn(id = 12L, conversationId = "", question = "Second", createdAt = 2_000L),
                ),
            )

        assertEquals(listOf("legacy-12", "legacy-11"), conversations.map { it.id })
    }

    private fun turn(
        id: Long,
        conversationId: String,
        question: String,
        createdAt: Long,
    ): AssistantChatTurn =
        AssistantChatTurn(
            id = id,
            question = question,
            answer = "Answer to $question",
            source = "CLOUD",
            interrupted = false,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            conversationId = conversationId,
        )
}
