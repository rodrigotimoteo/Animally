package com.github.rodrigotimoteo.animally.domain.anamnese.model

import kotlin.time.Instant

/**
 * Domain model representing the medical history of a patient.
 *
 * Anamnese is a 1:1 record with the patient: exactly one row per patient, containing
 * general medical history, chronic conditions, and allergies. It is distinct from
 * consultation-specific SOAP notes.
 *
 * @property id Unique identifier for the anamnese record.
 * @property patientId Identifier of the patient this record belongs to.
 * @property generalHistory Free-form general medical history.
 * @property chronicConditions Free-form chronic conditions.
 * @property allergies Free-form allergies.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class Anamnese(
    val id: Long,
    val patientId: Long,
    val generalHistory: String,
    val chronicConditions: String,
    val allergies: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
