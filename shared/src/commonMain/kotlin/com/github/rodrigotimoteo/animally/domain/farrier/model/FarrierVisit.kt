package com.github.rodrigotimoteo.animally.domain.farrier.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a farrier (hoof care) visit.
 *
 * @property id Unique identifier for the farrier visit.
 * @property patientId Identifier of the patient this visit belongs to.
 * @property date Date of the visit.
 * @property trimOrShoe Optional trim or shoeing type.
 * @property shoeType Optional shoe type applied.
 * @property findings Optional findings from the examination.
 * @property nextDueDate Optional date of the next scheduled visit.
 * @property farrier Optional name of the farrier.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class FarrierVisit(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val trimOrShoe: String? = null,
    val shoeType: String? = null,
    val findings: String? = null,
    val nextDueDate: LocalDate? = null,
    val farrier: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
