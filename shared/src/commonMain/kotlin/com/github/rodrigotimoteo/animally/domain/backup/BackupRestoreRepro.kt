package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.insertReproductionEvents(payload: BackupPayload) {
    payload.reproductionEvents.forEach { row ->
        reproductionQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            eventType = row.eventType,
            date = row.date,
            details = row.details,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            initialExamFindings = row.initialExamFindings,
            stallionName = row.stallionName,
            breedingType = row.breedingType,
        )
    }
}

internal fun AnimallyDatabase.insertUltrasounds(payload: BackupPayload) {
    payload.ultrasounds.forEach { row ->
        ultrasoundQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            ovaryStatus = row.ovaryStatus,
            uterineStatus = row.uterineStatus,
            follicleSizeMm = row.follicleSizeMm,
            leftOvaryStatus = row.leftOvaryStatus,
            rightOvaryStatus = row.rightOvaryStatus,
            leftFollicleSizeMm = row.leftFollicleSizeMm,
            rightFollicleSizeMm = row.rightFollicleSizeMm,
            uterineEdema = row.uterineEdema,
            uterineLiquid = row.uterineLiquid,
            uterineLiquidDescription = row.uterineLiquidDescription,
            uterusDescription = row.uterusDescription,
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

internal fun AnimallyDatabase.insertFollicles(payload: BackupPayload) {
    payload.follicles.forEach { row ->
        follicleQueries.insertWithId(
            id = row.id,
            ultrasoundId = row.ultrasoundId,
            side = row.side,
            sizeMm = row.sizeMm,
            description = row.description,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertEmbryoTransfers(payload: BackupPayload) {
    payload.embryoTransfers.forEach { row ->
        embryoTransferQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            embryoCount = row.embryoCount,
            recipientMares = row.recipientMares,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertIcsi(payload: BackupPayload) {
    payload.icsi.forEach { row ->
        icsiQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            date = row.date,
            folliclesRecovered = row.folliclesRecovered,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertGestations(payload: BackupPayload) {
    payload.gestations.forEach { row ->
        gestationQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            breedingDate = row.breedingDate,
            expectedDueDate = row.expectedDueDate,
            gestationDays = row.gestationDays,
            status = row.status,
            fetalCount = row.fetalCount,
            lastCheckDate = row.lastCheckDate,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertReproMedications(payload: BackupPayload) {
    payload.reproMedications.forEach { row ->
        reproMedicationQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            medication = row.medication,
            dateAdministered = row.dateAdministered,
            dosage = row.dosage,
            purpose = row.purpose,
            vetName = row.vetName,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

internal fun AnimallyDatabase.insertSubstances(payload: BackupPayload) {
    payload.substances.forEach { row ->
        substanceQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            drugName = row.drugName,
            dose = row.dose,
            unit = row.unit,
            route = row.route,
            administeredBy = row.administeredBy,
            witness = row.witness,
            date = row.date,
            reason = row.reason,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}
