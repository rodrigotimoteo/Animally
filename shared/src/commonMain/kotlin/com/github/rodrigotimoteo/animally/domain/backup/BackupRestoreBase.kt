package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

/**
 * Clears every persisted table so the restore can start from an empty state.
 */
internal fun AnimallyDatabase.deleteAllBackupRows() {
    anamneseQueries.deleteAll()
    consultationQueries.deleteAll()
    dentistryQueries.deleteAll()
    dewormingQueries.deleteAll()
    farrierVisitQueries.deleteAll()
    gestationQueries.deleteAll()
    imagingQueries.deleteAll()
    labResultQueries.deleteAll()
    lamenessQueries.deleteAll()
    medicationQueries.deleteAll()
    ownerQueries.deleteAll()
    patientQueries.deleteAll()
    reproMedicationQueries.deleteAll()
    reproductionQueries.deleteAll()
    substanceQueries.deleteAll()
    surgeryQueries.deleteAll()
    ultrasoundQueries.deleteAll()
    vaccinationQueries.deleteAll()
    weightQueries.deleteAll()
}

internal fun AnimallyDatabase.insertOwners(payload: BackupPayload) {
    payload.owners.forEach { row ->
        ownerQueries.insertWithId(
            id = row.id,
            name = row.name,
            email = row.email,
            phone = row.phone,
            address = row.address,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertPatients(payload: BackupPayload) {
    payload.patients.forEach { row ->
        patientQueries.insertWithId(
            id = row.id,
            name = row.name,
            species = row.species,
            breed = row.breed,
            dateOfBirth = row.dateOfBirth,
            gender = row.gender,
            microchipId = row.microchipId,
            ueln = row.ueln,
            registrationNumber = row.registrationNumber,
            stableLocation = row.stableLocation,
            photoUri = row.photoUri,
            notes = row.notes,
            ownerId = row.ownerId,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            cogginsTestDate = row.cogginsTestDate,
            cogginsResult = row.cogginsResult,
            cogginsExpiryDate = row.cogginsExpiryDate,
        )
    }
}

internal fun AnimallyDatabase.insertAnamnese(payload: BackupPayload) {
    payload.anamnese.forEach { row ->
        anamneseQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            generalHistory = row.generalHistory,
            chronicConditions = row.chronicConditions,
            allergies = row.allergies,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}
