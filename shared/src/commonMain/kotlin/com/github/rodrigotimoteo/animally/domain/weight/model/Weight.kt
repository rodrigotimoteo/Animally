package com.github.rodrigotimoteo.animally.domain.weight.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a historical weight measurement for a patient.
 *
 * Weight entries are tracked over time as separate records, not as a single value
 * on the patient, so the trend can be displayed.
 *
 * @property id Unique identifier for the weight entry.
 * @property patientId Identifier of the patient this entry belongs to.
 * @property weightKg The measured weight in kilograms.
 * @property date Date the weight was measured.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the entry is active. Defaults to `true`.
 * @property createdAt Timestamp when the entry was created.
 * @property updatedAt Timestamp when the entry was last modified.
 */
data class Weight(
    val id: Long,
    val patientId: Long,
    val weightKg: Double,
    val date: LocalDate,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
