package com.github.rodrigotimoteo.animally.domain.assistant

import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn

/** Persistence boundary for the assistant's local completed-turn history. */
interface IAssistantChatHistoryRepository {
    /** Returns the newest [limit] turns first. */
    fun getRecent(limit: Int): List<AssistantChatTurn>

    /** Inserts one completed turn and returns its generated id. */
    fun insert(turn: AssistantChatTurn): Long

    /** Keeps only the newest [limit] turns. */
    fun deleteOlderThan(limit: Int)

    /** Removes every persisted assistant turn. */
    fun deleteAll()
}
