package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.AssistantChatHistory
import com.github.rodrigotimoteo.animally.data.migrations.DictationCapture
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantWebSourcesCodec
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
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
    // Optional keeps backups created before web-reference cards backward-compatible.
    val webSources: List<VeterinaryWebSource> = emptyList(),
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
        webSources = AssistantWebSourcesCodec.decode(webSourcesJson),
    )

internal fun DictationCapture.toDto(): DictationCaptureDto =
    DictationCaptureDto(
        id = id,
        transcript = transcript,
        audioPath = audioPath,
        durationMillis = durationMillis,
        capturedAt = capturedAt,
    )
