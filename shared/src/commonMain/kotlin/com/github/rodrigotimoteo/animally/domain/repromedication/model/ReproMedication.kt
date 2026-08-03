package com.github.rodrigotimoteo.animally.domain.repromedication.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a reproduction-related medication administration.
 *
 * @property id Unique identifier for the medication record.
 * @property patientId Identifier of the patient this medication belongs to.
 * @property medication Name of the administered medication.
 * @property dateAdministered Date the medication was administered.
 * @property dosage Optional dosage information.
 * @property purpose Optional purpose of the medication.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the medication record is active. Defaults to `true`.
 * @property createdAt Timestamp when the medication record was created.
 * @property updatedAt Timestamp when the medication record was last modified.
 */
data class ReproMedication(
    val id: Long,
    val patientId: Long,
    val medication: String,
    val dateAdministered: LocalDate,
    val dosage: String? = null,
    val purpose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
