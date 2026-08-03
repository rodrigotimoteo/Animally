package com.github.rodrigotimoteo.animally.domain.vaccination.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a vaccination record.
 *
 * Vaccination records include the administered vaccine, the administration date,
 * and the next due date computed from the vaccine's interval. Vaccination is a
 * separate entity from Deworming — different drug class and reminder cadence.
 *
 * @property id Unique identifier for the vaccination record.
 * @property patientId Identifier of the patient this vaccination belongs to.
 * @property vaccineName Name of the administered vaccine.
 * @property dateAdministered Date the vaccine was administered.
 * @property nextDueDate Optional date the next dose is due.
 * @property vetName Optional name of the attending veterinarian.
 * @property batchNumber Optional batch number of the vaccine.
 * @property site Optional administration site.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the vaccination record is active. Defaults to `true`.
 * @property createdAt Timestamp when the vaccination was created.
 * @property updatedAt Timestamp when the vaccination was last modified.
 */
data class Vaccination(
    val id: Long,
    val patientId: Long,
    val vaccineName: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val vetName: String? = null,
    val batchNumber: String? = null,
    val site: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
