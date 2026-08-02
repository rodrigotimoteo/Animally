package com.github.rodrigotimoteo.animally.domain.substance.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a controlled substance administration to a patient.
 *
 * Controlled substances are subject to regulatory tracking: every administration is
 * logged with the dose, the administering veterinarian, and a witness.
 *
 * @property id Unique identifier for the controlled substance record.
 * @property patientId Identifier of the patient this controlled substance belongs to.
 * @property drugName Name of the controlled substance drug.
 * @property dose Dose administered.
 * @property unit Optional dose unit.
 * @property route Optional route of administration.
 * @property administeredBy Optional name of the veterinarian who administered the drug.
 * @property witness Optional name of the witness.
 * @property date Date of administration.
 * @property reason Optional reason for administration.
 * @property notes Optional notes.
 * @property isActive Indicates whether the controlled substance record is active. Defaults to `true`.
 * @property createdAt Timestamp when the controlled substance record was created.
 * @property updatedAt Timestamp when the controlled substance record was last modified.
 */
data class ControlledSubstance(
    val id: Long,
    val patientId: Long,
    val drugName: String,
    val dose: String,
    val unit: String? = null,
    val route: String? = null,
    val administeredBy: String? = null,
    val witness: String? = null,
    val date: LocalDate,
    val reason: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
