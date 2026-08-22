package com.github.rodrigotimoteo.animally.domain.ultrasound.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing a reproductive ultrasound examination.
 *
 * Carries structured reproductive fields: ovary status, uterine status and
 * follicle size in millimeters.
 *
 * @property id Unique identifier for the ultrasound.
 * @property patientId Identifier of the patient this ultrasound belongs to.
 * @property date Date of the examination.
 * @property ovaryStatus Optional status of the ovaries.
 * @property uterineStatus Optional status of the uterus.
 * @property follicleSizeMm Optional follicle size in millimeters.
 * @property findings Optional findings from the examination.
 * @property imageUris Optional comma-separated URIs of attached images.
 * @property vetName Optional name of the attending veterinarian.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the ultrasound record is active. Defaults to `true`.
 * @property createdAt Timestamp when the ultrasound was created.
 * @property updatedAt Timestamp when the ultrasound was last modified.
 */
data class Ultrasound(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val ovaryStatus: String? = null,
    val uterineStatus: String? = null,
    val follicleSizeMm: Double? = null,
    val leftOvaryStatus: String? = null,
    val rightOvaryStatus: String? = null,
    val leftFollicleSizeMm: Double? = null,
    val rightFollicleSizeMm: Double? = null,
    val uterineEdema: String? = null,
    val uterineLiquid: Boolean? = null,
    val uterineLiquidDescription: String? = null,
    val uterusDescription: String? = null,
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
