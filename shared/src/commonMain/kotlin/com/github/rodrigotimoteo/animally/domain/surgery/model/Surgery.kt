package com.github.rodrigotimoteo.animally.domain.surgery.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a surgery performed on a patient.
 *
 * @property id Unique identifier for the surgery record.
 * @property patientId Identifier of the patient this surgery belongs to.
 * @property date Date of the surgery.
 * @property type Optional type of surgery performed.
 * @property description Optional description of the procedure.
 * @property outcome Optional post-surgery outcome.
 * @property surgeon Optional name of the surgeon.
 * @property anesthesia Optional anesthesia details.
 * @property analgesia Optional analgesia details.
 * @property complications Optional complications encountered.
 * @property recoveryNotes Optional recovery notes.
 * @property isActive Indicates whether the surgery record is active. Defaults to `true`.
 * @property createdAt Timestamp when the surgery was created.
 * @property updatedAt Timestamp when the surgery was last modified.
 */
data class Surgery(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val type: String? = null,
    val description: String? = null,
    val outcome: String? = null,
    val surgeon: String? = null,
    val anesthesia: String? = null,
    val analgesia: String? = null,
    val complications: String? = null,
    val recoveryNotes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
