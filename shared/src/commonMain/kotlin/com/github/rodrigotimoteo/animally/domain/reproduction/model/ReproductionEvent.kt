package com.github.rodrigotimoteo.animally.domain.reproduction.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a reproduction-cycle event for a patient.
 *
 * Covers breeding-cycle events such as heat, breeding, pregnancy check and foaling.
 *
 * @property id Unique identifier for the reproduction event.
 * @property patientId Identifier of the patient this event belongs to.
 * @property eventType Type of the reproduction event.
 * @property date Date of the event.
 * @property details Optional additional details about the event.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the event record is active. Defaults to `true`.
 * @property createdAt Timestamp when the event was created.
 * @property updatedAt Timestamp when the event was last modified.
 */
data class ReproductionEvent(
    val id: Long,
    val patientId: Long,
    val eventType: String,
    val date: LocalDate,
    val details: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
