package com.github.rodrigotimoteo.animally.domain.assistant.model

/** A chronological group of assistant turns from one local chat session. */
data class AssistantConversation(
    val id: String,
    val turns: List<AssistantChatTurn>,
) {
    init {
        require(turns.isNotEmpty()) { "A conversation must contain at least one turn" }
    }

    /** The first question is a useful, stable label for the conversation. */
    val title: String
        get() = turns.firstOrNull()?.question ?: "New conversation"

    /** The latest answer gives the conversation list a useful preview. */
    val preview: String
        get() = turns.lastOrNull()?.answer.orEmpty()

    /** When the conversation began. */
    val createdAt
        get() = turns.first().createdAt

    /** When the conversation was most recently updated. */
    val updatedAt
        get() = turns.last().createdAt
}

/** Returns the persisted conversation key, with a safe key for legacy rows. */
fun AssistantChatTurn.conversationKey(): String =
    conversationId
        .trim()
        .ifEmpty { "legacy-$id" }

/** Groups persisted turns without allowing unrelated chats to be flattened together. */
object AssistantConversationGrouper {
    /** Returns conversations newest-first while keeping turns oldest-first within each block. */
    fun group(turns: List<AssistantChatTurn>): List<AssistantConversation> =
        turns
            .groupBy { it.conversationKey() }
            .values
            .map { group ->
                val ordered = group.sortedWith(compareBy<AssistantChatTurn> { it.createdAt }.thenBy { it.id })
                AssistantConversation(
                    id = ordered.first().conversationKey(),
                    turns = ordered,
                )
            }.sortedWith(
                compareByDescending<AssistantConversation> { it.updatedAt }
                    .thenByDescending { it.id },
            )
}
