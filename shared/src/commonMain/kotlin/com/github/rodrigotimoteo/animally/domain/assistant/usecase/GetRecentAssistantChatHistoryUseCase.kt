package com.github.rodrigotimoteo.animally.domain.assistant.usecase

import com.github.rodrigotimoteo.animally.domain.assistant.IAssistantChatHistoryRepository
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

private const val MAX_TURNS = SaveAssistantChatTurnUseCase.MAX_HISTORY_TURNS

/** Loads the bounded local assistant history in chronological order for the UI. */
@Single
class GetRecentAssistantChatHistoryUseCase(
    @Provided private val repository: IAssistantChatHistoryRepository,
) {
    /** Returns up to [limit] turns, oldest first. */
    operator fun invoke(limit: Int = MAX_TURNS): List<AssistantChatTurn> = repository.getRecent(limit).asReversed()
}
