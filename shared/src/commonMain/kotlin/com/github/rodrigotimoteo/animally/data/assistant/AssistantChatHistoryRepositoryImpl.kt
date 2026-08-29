package com.github.rodrigotimoteo.animally.data.assistant

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.assistant.IAssistantChatHistoryRepository
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantWebSourcesCodec
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** SQLDelight implementation of the local assistant history store. */
@Single(binds = [IAssistantChatHistoryRepository::class])
class AssistantChatHistoryRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
    @Provided private val queries: AssistantChatHistoryQueries,
) : IAssistantChatHistoryRepository {
    override fun getRecent(limit: Int): List<AssistantChatTurn> =
        queries
            .selectRecent(limit.toLong())
            .executeAsList()
            .map { row ->
                AssistantChatTurn(
                    id = row.id,
                    question = row.question,
                    answer = row.answer,
                    source = row.source,
                    interrupted = row.interrupted,
                    createdAt = row.createdAt,
                    conversationId = row.conversationId.ifBlank { "legacy-${row.id}" },
                    webSources = AssistantWebSourcesCodec.decode(row.webSourcesJson),
                )
            }

    override fun insert(turn: AssistantChatTurn): Long =
        database.transactionWithResult {
            queries.insert(
                question = turn.question,
                answer = turn.answer,
                source = turn.source,
                interrupted = turn.interrupted,
                createdAt = turn.createdAt,
                conversationId = turn.conversationId,
                webSourcesJson = AssistantWebSourcesCodec.encode(turn.webSources),
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun deleteOlderThan(limit: Int) {
        queries.deleteOlderThan(limit.toLong()).value
    }

    override fun deleteAll() {
        queries.deleteAll().value
    }
}
