package com.github.rodrigotimoteo.animally.domain.icsi.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing an ICSI (intracytoplasmic sperm injection)
 * record for a mare.
 *
 * Tracks how many follicles were recovered for the ICSI procedure.
 *
 * @property id Unique identifier for the ICSI record.
 * @property patientId Identifier of the patient this record belongs to.
 * @property date Date of the follicle recovery / ICSI procedure.
 * @property folliclesRecovered Number of follicles recovered.
 * @property vetName Optional name of the veterinarian performing the procedure.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class Icsi(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val folliclesRecovered: Int = 0,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
