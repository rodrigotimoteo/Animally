package com.github.rodrigotimoteo.animally.domain.assistant.model

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlin.time.Instant

/** A completed assistant question/answer turn retained for local history. */
data class AssistantChatTurn(
    val id: Long = 0L,
    val question: String,
    val answer: String,
    val source: String,
    val interrupted: Boolean,
    val createdAt: Instant,
    /** Stable local boundary used to keep related turns in one conversation. */
    val conversationId: String = "",
    /** Public references shown with a general medical answer. */
    val webSources: List<VeterinaryWebSource> = emptyList(),
    /** Minimal local identities used to reopen cited patient records. */
    val recordSources: List<AssistantRecordSource> = emptyList(),
)
