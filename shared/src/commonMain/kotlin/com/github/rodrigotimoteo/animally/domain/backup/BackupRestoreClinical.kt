package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.insertSurgeries(payload: BackupPayload) {
    payload.surgeries.forEach { row ->
        surgeryQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            type = row.type,
            description = row.description,
            outcome = row.outcome,
            surgeon = row.surgeon,
            anesthesia = row.anesthesia,
            analgesia = row.analgesia,
            complications = row.complications,
            recoveryNotes = row.recoveryNotes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertMedications(payload: BackupPayload) {
    payload.medications.forEach { row ->
        medicationQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            name = row.name,
            dosage = row.dosage,
            route = row.route,
            frequency = row.frequency,
            startDate = row.startDate,
            endDate = row.endDate,
            prescribedBy = row.prescribedBy,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertLabResults(payload: BackupPayload) {
    payload.labResults.forEach { row ->
        labResultQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            testType = row.testType,
            date = row.date,
            results = row.results,
            normalRange = row.normalRange,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertImaging(payload: BackupPayload) {
    payload.imaging.forEach { row ->
        imagingQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            type = row.type,
            date = row.date,
            findings = row.findings,
            imageUris = row.imageUris,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertFarrierVisits(payload: BackupPayload) {
    payload.farrierVisits.forEach { row ->
        farrierVisitQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            trimOrShoe = row.trimOrShoe,
            shoeType = row.shoeType,
            findings = row.findings,
            nextDueDate = row.nextDueDate,
            farrier = row.farrier,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}
