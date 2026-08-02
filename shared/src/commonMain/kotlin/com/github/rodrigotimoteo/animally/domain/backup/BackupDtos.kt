package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.migrations.Anamnese
import com.github.rodrigotimoteo.animally.data.migrations.Owner
import com.github.rodrigotimoteo.animally.data.migrations.Patient
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Serializable mirror of the Patient table.
 */
@Serializable
data class PatientDto(
    val id: Long,
    val name: String,
    val species: String,
    val breed: String?,
    @Serializable(with = LocalDateSerializer::class) val dateOfBirth: LocalDate?,
    val gender: String?,
    val microchipId: String?,
    val ueln: String?,
    val registrationNumber: String?,
    val stableLocation: String?,
    val photoUri: String?,
    val notes: String?,
    val ownerId: Long?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
    @Serializable(with = LocalDateSerializer::class) val cogginsTestDate: LocalDate?,
    val cogginsResult: String?,
    @Serializable(with = LocalDateSerializer::class) val cogginsExpiryDate: LocalDate?,
)

/**
 * Serializable mirror of the Owner table.
 */
@Serializable
data class OwnerDto(
    val id: Long,
    val name: String,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

/**
 * Serializable mirror of the Anamnese table.
 */
@Serializable
data class AnamneseDto(
    val id: Long,
    val patientId: Long,
    val generalHistory: String?,
    val chronicConditions: String?,
    val allergies: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

internal fun Patient.toDto(): PatientDto =
    PatientDto(
        id = id,
        name = name,
        species = species,
        breed = breed,
        dateOfBirth = dateOfBirth,
        gender = gender,
        microchipId = microchipId,
        ueln = ueln,
        registrationNumber = registrationNumber,
        stableLocation = stableLocation,
        photoUri = photoUri,
        notes = notes,
        ownerId = ownerId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        cogginsTestDate = cogginsTestDate,
        cogginsResult = cogginsResult,
        cogginsExpiryDate = cogginsExpiryDate,
    )

internal fun Owner.toDto(): OwnerDto =
    OwnerDto(
        id = id,
        name = name,
        email = email,
        phone = phone,
        address = address,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Anamnese.toDto(): AnamneseDto =
    AnamneseDto(
        id = id,
        patientId = patientId,
        generalHistory = generalHistory,
        chronicConditions = chronicConditions,
        allergies = allergies,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
