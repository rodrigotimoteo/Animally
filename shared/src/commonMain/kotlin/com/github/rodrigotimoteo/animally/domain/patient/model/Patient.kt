package com.github.rodrigotimoteo.animally.domain.patient.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing an animal (equine) under veterinary care.
 *
 * @property id Unique identifier for the patient.
 * @property name The patient's name.
 * @property species The species of the patient. Defaults to `Equine`.
 * @property breed Optional breed of the patient.
 * @property dateOfBirth Optional date of birth of the patient.
 * @property gender Optional gender of the patient.
 * @property microchipId Optional microchip identifier.
 * @property ueln Optional Unique Equine Life Number.
 * @property registrationNumber Optional studbook or federation registration number.
 * @property stableLocation Optional location where the patient is stabled.
 * @property photoUri Optional URI to a photo of the patient.
 * @property notes Optional free-form notes about the patient.
 * @property cogginsTestDate Optional date of the most recent Coggins (Equine Infectious Anemia) test.
 * @property cogginsResult Optional result of the most recent Coggins test.
 * @property cogginsExpiryDate Optional expiry date of the most recent Coggins test.
 * @property ownerId Identifier of the owning owner, or `null` when unassigned.
 * @property isActive Indicates whether the patient record is active. Defaults to `true`.
 * @property createdAt Timestamp when the patient record was created.
 * @property updatedAt Timestamp when the patient record was last modified.
 */
data class Patient(
    val id: Long,
    val name: String,
    val species: String = "Equine",
    val breed: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val microchipId: String? = null,
    val ueln: String? = null,
    val registrationNumber: String? = null,
    val stableLocation: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val cogginsTestDate: LocalDate? = null,
    val cogginsResult: String? = null,
    val cogginsExpiryDate: LocalDate? = null,
    val ownerId: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
