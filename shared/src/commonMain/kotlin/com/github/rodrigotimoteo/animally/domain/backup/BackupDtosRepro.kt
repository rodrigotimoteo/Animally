package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.Gestation
import com.github.rodrigotimoteo.animally.data.migrations.ReproMedication
import com.github.rodrigotimoteo.animally.data.migrations.Reproduction
import com.github.rodrigotimoteo.animally.data.migrations.Substance
import com.github.rodrigotimoteo.animally.data.migrations.Ultrasound
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Serializable mirror of the Reproduction table.
 */
@Serializable
data class ReproductionEventDto(
    val id: Long,
    val patientId: Long,
    val eventType: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val details: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Ultrasound table.
 */
@Serializable
data class UltrasoundDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val ovaryStatus: String?,
    val uterineStatus: String?,
    val follicleSizeMm: Double?,
    val findings: String?,
    val imageUris: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Gestation table.
 */
@Serializable
data class GestationDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val breedingDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val expectedDueDate: LocalDate,
    val gestationDays: Long,
    val status: String?,
    val fetalCount: Long?,
    @Serializable(with = LocalDateSerializer::class) val lastCheckDate: LocalDate?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the ReproMedication table.
 */
@Serializable
data class ReproMedicationDto(
    val id: Long,
    val patientId: Long,
    val medication: String,
    @Serializable(with = LocalDateSerializer::class) val dateAdministered: LocalDate,
    val dosage: String?,
    val purpose: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Substance (controlled substance log) table.
 */
@Serializable
data class ControlledSubstanceDto(
    val id: Long,
    val patientId: Long,
    val drugName: String,
    val dose: String,
    val unit: String?,
    val route: String?,
    val administeredBy: String?,
    val witness: String?,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val reason: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

internal fun Reproduction.toDto(): ReproductionEventDto =
    ReproductionEventDto(
        id = id,
        patientId = patientId,
        eventType = eventType,
        date = date,
        details = details,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Ultrasound.toDto(): UltrasoundDto =
    UltrasoundDto(
        id = id,
        patientId = patientId,
        date = date,
        ovaryStatus = ovaryStatus,
        uterineStatus = uterineStatus,
        follicleSizeMm = follicleSizeMm,
        findings = findings,
        imageUris = imageUris,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Gestation.toDto(): GestationDto =
    GestationDto(
        id = id,
        patientId = patientId,
        breedingDate = breedingDate,
        expectedDueDate = expectedDueDate,
        gestationDays = gestationDays,
        status = status,
        fetalCount = fetalCount,
        lastCheckDate = lastCheckDate,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun ReproMedication.toDto(): ReproMedicationDto =
    ReproMedicationDto(
        id = id,
        patientId = patientId,
        medication = medication,
        dateAdministered = dateAdministered,
        dosage = dosage,
        purpose = purpose,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Substance.toDto(): ControlledSubstanceDto =
    ControlledSubstanceDto(
        id = id,
        patientId = patientId,
        drugName = drugName,
        dose = dose,
        unit = unit,
        route = route,
        administeredBy = administeredBy,
        witness = witness,
        date = date,
        reason = reason,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
