package com.github.rodrigotimoteo.animally.domain.assistant.model

import kotlin.time.Instant

/** A completed assistant question/answer turn retained for local history. */
data class AssistantChatTurn(
    val id: Long = 0L,
    val question: String,
    val answer: String,
    val source: String,
    val interrupted: Boolean,
    val createdAt: Instant,
)
