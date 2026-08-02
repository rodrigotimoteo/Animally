package com.github.rodrigotimoteo.animally.data.patient.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Patient
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient as DomainPatient

/**
 * Converts this persistence [Patient] to a domain [DomainPatient].
 *
 * @return mapped [DomainPatient]
 */
fun Patient.toDomain(): DomainPatient =
    DomainPatient(
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
        cogginsTestDate = cogginsTestDate,
        cogginsResult = cogginsResult,
        cogginsExpiryDate = cogginsExpiryDate,
        ownerId = ownerId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
