package com.github.rodrigotimoteo.animally.domain.deworming.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a deworming (anthelmintic) treatment record.
 *
 * Deworming is a separate entity from Vaccination — different drug class and
 * different reminder cadence.
 *
 * @property id Unique identifier for the deworming record.
 * @property patientId Identifier of the patient this deworming belongs to.
 * @property product Name of the anthelmintic product.
 * @property dateAdministered Date the deworming was administered.
 * @property nextDueDate Optional date the next deworming is due.
 * @property dose Optional dosage information.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class Deworming(
    val id: Long,
    val patientId: Long,
    val product: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val dose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
