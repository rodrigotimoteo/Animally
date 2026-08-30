package com.github.rodrigotimoteo.animally.domain.assistant.model

import kotlinx.serialization.Serializable

/**
 * Minimal persisted identity for a record source cited by an assistant turn.
 *
 * The full search snippet is intentionally not retained in chat history. These
 * fields are enough for the presentation layer to reopen the authoritative
 * patient/record detail after the turn has been reloaded.
 */
@Serializable
data class AssistantRecordSource(
    val patientId: Long,
    val patientName: String,
    val recordType: String,
    val recordId: Long,
    val date: String? = null,
)
