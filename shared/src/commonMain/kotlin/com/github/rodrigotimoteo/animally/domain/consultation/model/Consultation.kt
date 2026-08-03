package com.github.rodrigotimoteo.animally.domain.consultation.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a veterinary consultation documented via SOAP notes.
 *
 * One consultation represents one visit. The clinical documentation is structured
 * as SOAP: Subjective (owner's description), Objective (exam findings),
 * Assessment (diagnosis), and Plan (treatment).
 *
 * @property id Unique identifier for the consultation.
 * @property patientId Identifier of the patient this consultation belongs to.
 * @property date Date of the consultation.
 * @property subjective SOAP Subjective — the owner's description of the issue.
 * @property objective SOAP Objective — the exam findings.
 * @property assessment SOAP Assessment — the diagnosis.
 * @property plan SOAP Plan — the treatment.
 * @property vetName Optional name of the attending veterinarian.
 * @property nextVisitDate Optional date of the next scheduled visit.
 * @property isActive Indicates whether the consultation record is active. Defaults to `true`.
 * @property createdAt Timestamp when the consultation was created.
 * @property updatedAt Timestamp when the consultation was last modified.
 */
data class Consultation(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val subjective: String,
    val objective: String,
    val assessment: String,
    val plan: String,
    val vetName: String? = null,
    val nextVisitDate: LocalDate? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
