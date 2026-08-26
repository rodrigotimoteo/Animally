package com.github.rodrigotimoteo.animally.domain.assistant.usecase

import com.github.rodrigotimoteo.animally.domain.assistant.IAssistantChatHistoryRepository
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Stores a completed turn and enforces the product's local history limit. */
@Single
class SaveAssistantChatTurnUseCase(
    @Provided private val repository: IAssistantChatHistoryRepository,
) {
    /** Persists [turn], then removes turns older than the newest 15. */
    operator fun invoke(turn: AssistantChatTurn) {
        repository.insert(turn)
        repository.deleteOlderThan(MAX_HISTORY_TURNS)
    }

    companion object {
        /** Number of completed assistant turns retained on the device. */
        const val MAX_HISTORY_TURNS = 15
    }
}
