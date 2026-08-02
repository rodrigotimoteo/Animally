package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.FarrierVisit
import com.github.rodrigotimoteo.animally.data.migrations.Imaging
import com.github.rodrigotimoteo.animally.data.migrations.LabResult
import com.github.rodrigotimoteo.animally.data.migrations.Medication
import com.github.rodrigotimoteo.animally.data.migrations.Surgery
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Serializable mirror of the Surgery table.
 */
@Serializable
data class SurgeryDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val type: String?,
    val description: String?,
    val outcome: String?,
    val surgeon: String?,
    val anesthesia: String?,
    val analgesia: String?,
    val complications: String?,
    val recoveryNotes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Medication table.
 */
@Serializable
data class MedicationDto(
    val id: Long,
    val patientId: Long,
    val name: String,
    val dosage: String,
    val route: String?,
    val frequency: String?,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate?,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate?,
    val prescribedBy: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the LabResult table.
 */
@Serializable
data class LabResultDto(
    val id: Long,
    val patientId: Long,
    val testType: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val results: String?,
    val normalRange: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Imaging table.
 */
@Serializable
data class ImagingDto(
    val id: Long,
    val patientId: Long,
    val type: String?,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val findings: String?,
    val imageUris: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the FarrierVisit table.
 */
@Serializable
data class FarrierVisitDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val trimOrShoe: String?,
    val shoeType: String?,
    val findings: String?,
    @Serializable(with = LocalDateSerializer::class) val nextDueDate: LocalDate?,
    val farrier: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

internal fun Surgery.toDto(): SurgeryDto =
    SurgeryDto(
        id = id,
        patientId = patientId,
        date = date,
        type = type,
        description = description,
        outcome = outcome,
        surgeon = surgeon,
        anesthesia = anesthesia,
        analgesia = analgesia,
        complications = complications,
        recoveryNotes = recoveryNotes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Medication.toDto(): MedicationDto =
    MedicationDto(
        id = id,
        patientId = patientId,
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        prescribedBy = prescribedBy,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun LabResult.toDto(): LabResultDto =
    LabResultDto(
        id = id,
        patientId = patientId,
        testType = testType,
        date = date,
        results = results,
        normalRange = normalRange,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Imaging.toDto(): ImagingDto =
    ImagingDto(
        id = id,
        patientId = patientId,
        type = type,
        date = date,
        findings = findings,
        imageUris = imageUris,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun FarrierVisit.toDto(): FarrierVisitDto =
    FarrierVisitDto(
        id = id,
        patientId = patientId,
        date = date,
        trimOrShoe = trimOrShoe,
        shoeType = shoeType,
        findings = findings,
        nextDueDate = nextDueDate,
        farrier = farrier,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
