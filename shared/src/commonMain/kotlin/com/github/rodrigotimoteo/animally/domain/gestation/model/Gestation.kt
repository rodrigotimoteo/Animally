package com.github.rodrigotimoteo.animally.domain.gestation.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a pregnancy (gestation) record for a patient.
 *
 * Tracks the breeding date, an automatically computed expected due date and the
 * current gestation day count.
 *
 * @property id Unique identifier for the gestation record.
 * @property patientId Identifier of the patient this gestation belongs to.
 * @property breedingDate Date the mare was bred.
 * @property expectedDueDate Computed expected foaling date.
 * @property gestationDays Current number of days since breeding.
 * @property status Current status of the pregnancy.
 * @property fetalCount Optional number of fetuses.
 * @property lastCheckDate Optional date of the last pregnancy check.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the gestation record is active. Defaults to `true`.
 * @property createdAt Timestamp when the gestation was created.
 * @property updatedAt Timestamp when the gestation was last modified.
 */
data class Gestation(
    val id: Long,
    val patientId: Long,
    val breedingDate: LocalDate,
    val expectedDueDate: LocalDate,
    val gestationDays: Int,
    val status: String,
    val fetalCount: Int? = null,
    val lastCheckDate: LocalDate? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
