package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.insertConsultations(payload: BackupPayload) {
    payload.consultations.forEach { row ->
        consultationQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            subjective = row.subjective,
            objective = row.objective,
            assessment = row.assessment,
            plan = row.plan,
            vetName = row.vetName,
            nextVisitDate = row.nextVisitDate,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertVaccinations(payload: BackupPayload) {
    payload.vaccinations.forEach { row ->
        vaccinationQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            vaccineName = row.vaccineName,
            dateAdministered = row.dateAdministered,
            nextDueDate = row.nextDueDate,
            vetName = row.vetName,
            batchNumber = row.batchNumber,
            site = row.site,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertWeights(payload: BackupPayload) {
    payload.weights.forEach { row ->
        weightQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            weightKg = row.weightKg,
            date = row.date,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertDewormings(payload: BackupPayload) {
    payload.dewormings.forEach { row ->
        dewormingQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            product = row.product,
            dateAdministered = row.dateAdministered,
            nextDueDate = row.nextDueDate,
            dose = row.dose,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertDentistry(payload: BackupPayload) {
    payload.dentistry.forEach { row ->
        dentistryQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            findings = row.findings,
            treatment = row.treatment,
            nextDueDate = row.nextDueDate,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertLameness(payload: BackupPayload) {
    payload.lameness.forEach { row ->
        lamenessQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            gradeAAEP = row.gradeAAEP,
            limbLocation = row.limbLocation,
            flexionTest = row.flexionTest,
            diagnosis = row.diagnosis,
            treatment = row.treatment,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}
