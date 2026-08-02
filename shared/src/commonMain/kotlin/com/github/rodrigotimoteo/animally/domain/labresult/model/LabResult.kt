package com.github.rodrigotimoteo.animally.domain.labresult.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a laboratory test result for a patient.
 *
 * @property id Unique identifier for the lab result.
 * @property patientId Identifier of the patient this lab result belongs to.
 * @property testType Type of laboratory test performed.
 * @property date Date the test was performed.
 * @property results Optional test result values.
 * @property normalRange Optional reference range for the test.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-text notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the lab result was created.
 * @property updatedAt Timestamp when the lab result was last modified.
 */
data class LabResult(
    val id: Long,
    val patientId: Long,
    val testType: String,
    val date: LocalDate,
    val results: String? = null,
    val normalRange: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
