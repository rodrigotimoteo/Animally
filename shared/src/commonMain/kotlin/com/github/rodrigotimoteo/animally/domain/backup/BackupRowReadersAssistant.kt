package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.assistantChatHistoryRows(): List<AssistantChatHistoryDto> =
    assistantChatHistoryQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.dictationCaptureRows(): List<DictationCaptureDto> =
    dictationCaptureQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }
