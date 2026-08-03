package com.github.rodrigotimoteo.animally.domain.medication.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a medication prescribed to a patient.
 *
 * @property id Unique identifier for the medication record.
 * @property patientId Identifier of the patient this medication belongs to.
 * @property name Name of the medication.
 * @property dosage Dosage of the medication.
 * @property route Optional route of administration.
 * @property frequency Optional administration frequency.
 * @property startDate Optional date the medication was started.
 * @property endDate Optional date the medication ended.
 * @property prescribedBy Optional name of the prescribing veterinarian.
 * @property notes Optional notes.
 * @property isActive Indicates whether the medication record is active. Defaults to `true`.
 * @property createdAt Timestamp when the medication was created.
 * @property updatedAt Timestamp when the medication was last modified.
 */
data class Medication(
    val id: Long,
    val patientId: Long,
    val name: String,
    val dosage: String,
    val route: String? = null,
    val frequency: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val prescribedBy: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
