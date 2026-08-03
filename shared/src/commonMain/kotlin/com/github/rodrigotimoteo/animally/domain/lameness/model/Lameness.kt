package com.github.rodrigotimoteo.animally.domain.lameness.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a lameness evaluation for a patient.
 *
 * Uses the AAEP 1-5 grading scale for lameness severity, along with the affected
 * limb location and any flexion test results.
 *
 * @property id Unique identifier for the lameness record.
 * @property patientId Identifier of the patient this lameness belongs to.
 * @property date Date of the lameness evaluation.
 * @property gradeAAEP AAEP lameness grade on the 1-5 scale.
 * @property limbLocation Optional affected limb location.
 * @property flexionTest Optional flexion test result.
 * @property diagnosis Optional diagnosis.
 * @property treatment Optional treatment plan.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional clinical notes.
 * @property isActive Indicates whether the lameness record is active. Defaults to `true`.
 * @property createdAt Timestamp when the lameness was created.
 * @property updatedAt Timestamp when the lameness was last modified.
 */
data class Lameness(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val gradeAAEP: Int,
    val limbLocation: String? = null,
    val flexionTest: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
