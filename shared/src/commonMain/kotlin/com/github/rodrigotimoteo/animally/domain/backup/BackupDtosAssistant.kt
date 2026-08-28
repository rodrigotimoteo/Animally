package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.AssistantChatHistory
import com.github.rodrigotimoteo.animally.data.migrations.DictationCapture
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Serializable mirror of the local assistant-history table. */
@Serializable
data class AssistantChatHistoryDto(
    val id: Long,
    val question: String,
    val answer: String,
    val source: String,
    val interrupted: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    // Optional keeps backups created before conversation blocks backward-compatible.
    val conversationId: String = "",
)

/** Serializable mirror of the dictation metadata table. */
@Serializable
data class DictationCaptureDto(
    val id: Long,
    val transcript: String,
    val audioPath: String?,
    val durationMillis: Long?,
    @Serializable(with = InstantSerializer::class) val capturedAt: Instant,
)

internal fun AssistantChatHistory.toDto(): AssistantChatHistoryDto =
    AssistantChatHistoryDto(
        id = id,
        question = question,
        answer = answer,
        source = source,
        interrupted = interrupted,
        createdAt = createdAt,
        conversationId = conversationId.ifBlank { "legacy-$id" },
    )

internal fun DictationCapture.toDto(): DictationCaptureDto =
    DictationCaptureDto(
        id = id,
        transcript = transcript,
        audioPath = audioPath,
        durationMillis = durationMillis,
        capturedAt = capturedAt,
    )
