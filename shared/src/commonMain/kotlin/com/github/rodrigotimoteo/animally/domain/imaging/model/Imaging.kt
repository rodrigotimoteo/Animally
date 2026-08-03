package com.github.rodrigotimoteo.animally.domain.imaging.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a diagnostic imaging record for a patient.
 *
 * @property id Unique identifier for the imaging record.
 * @property patientId Identifier of the patient this imaging record belongs to.
 * @property type Type of imaging study performed (e.g., X-ray, ultrasound).
 * @property date Date the imaging was performed.
 * @property findings Optional interpretation of the imaging.
 * @property imageUris Optional comma-separated list of image file paths.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-text notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the imaging record was created.
 * @property updatedAt Timestamp when the imaging record was last modified.
 */
data class Imaging(
    val id: Long,
    val patientId: Long,
    val type: String,
    val date: LocalDate,
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
