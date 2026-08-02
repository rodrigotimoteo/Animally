package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.Consultation
import com.github.rodrigotimoteo.animally.data.migrations.Dentistry
import com.github.rodrigotimoteo.animally.data.migrations.Deworming
import com.github.rodrigotimoteo.animally.data.migrations.Lameness
import com.github.rodrigotimoteo.animally.data.migrations.Vaccination
import com.github.rodrigotimoteo.animally.data.migrations.Weight
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Serializable mirror of the Consultation table (SOAP visit).
 */
@Serializable
data class ConsultationDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val subjective: String?,
    val objective: String?,
    val assessment: String?,
    val plan: String?,
    val vetName: String?,
    @Serializable(with = LocalDateSerializer::class) val nextVisitDate: LocalDate?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Vaccination table.
 */
@Serializable
data class VaccinationDto(
    val id: Long,
    val patientId: Long,
    val vaccineName: String,
    @Serializable(with = LocalDateSerializer::class) val dateAdministered: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val nextDueDate: LocalDate?,
    val vetName: String?,
    val batchNumber: String?,
    val site: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Weight table.
 */
@Serializable
data class WeightDto(
    val id: Long,
    val patientId: Long,
    val weightKg: Double,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Deworming table.
 */
@Serializable
data class DewormingDto(
    val id: Long,
    val patientId: Long,
    val product: String,
    @Serializable(with = LocalDateSerializer::class) val dateAdministered: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val nextDueDate: LocalDate?,
    val dose: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Dentistry table.
 */
@Serializable
data class DentistryDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val findings: String?,
    val treatment: String?,
    @Serializable(with = LocalDateSerializer::class) val nextDueDate: LocalDate?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Lameness table.
 */
@Serializable
data class LamenessDto(
    val id: Long,
    val patientId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val gradeAAEP: Long?,
    val limbLocation: String?,
    val flexionTest: String?,
    val diagnosis: String?,
    val treatment: String?,
    val vetName: String?,
    val notes: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

internal fun Consultation.toDto(): ConsultationDto =
    ConsultationDto(
        id = id,
        patientId = patientId,
        date = date,
        subjective = subjective,
        objective = objective,
        assessment = assessment,
        plan = plan,
        vetName = vetName,
        nextVisitDate = nextVisitDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Vaccination.toDto(): VaccinationDto =
    VaccinationDto(
        id = id,
        patientId = patientId,
        vaccineName = vaccineName,
        dateAdministered = dateAdministered,
        nextDueDate = nextDueDate,
        vetName = vetName,
        batchNumber = batchNumber,
        site = site,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Weight.toDto(): WeightDto =
    WeightDto(
        id = id,
        patientId = patientId,
        weightKg = weightKg,
        date = date,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Deworming.toDto(): DewormingDto =
    DewormingDto(
        id = id,
        patientId = patientId,
        product = product,
        dateAdministered = dateAdministered,
        nextDueDate = nextDueDate,
        dose = dose,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Dentistry.toDto(): DentistryDto =
    DentistryDto(
        id = id,
        patientId = patientId,
        date = date,
        findings = findings,
        treatment = treatment,
        nextDueDate = nextDueDate,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Lameness.toDto(): LamenessDto =
    LamenessDto(
        id = id,
        patientId = patientId,
        date = date,
        gradeAAEP = gradeAAEP,
        limbLocation = limbLocation,
        flexionTest = flexionTest,
        diagnosis = diagnosis,
        treatment = treatment,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
