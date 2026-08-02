package com.github.rodrigotimoteo.animally.domain.dentistry.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a dental check and treatment record (e.g., floating).
 *
 * The next due date follows a 3/6/9/12 month schedule, entered manually on the
 * form rather than computed.
 *
 * @property id Unique identifier for the dentistry record.
 * @property patientId Identifier of the patient this record belongs to.
 * @property date Date of the dental check.
 * @property findings Optional findings from the examination.
 * @property treatment Optional treatment performed.
 * @property nextDueDate Optional date the next dental check is due.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class Dentistry(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val findings: String? = null,
    val treatment: String? = null,
    val nextDueDate: LocalDate? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
