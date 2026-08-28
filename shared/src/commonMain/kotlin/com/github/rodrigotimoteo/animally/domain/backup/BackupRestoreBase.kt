package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import com.github.rodrigotimoteo.animally.domain.owner.model.OwnerLocation

/** Returns app-owned audio paths referenced by the current dictation rows. */
internal fun AnimallyDatabase.dictationAudioPaths(): Set<String> =
    dictationCaptureQueries
        .selectAll()
        .executeAsList()
        .mapNotNull { it.audioPath }
        .toSet()

/** Best-effort cleanup for app-owned dictation audio files. */
internal fun Iterable<String>.deleteDictationAudioFiles() {
    forEach { path ->
        runCatching { FileStorage.delete(path) }
    }
}

/**
 * Clears every persisted table so the restore can start from an empty state.
 */
internal fun AnimallyDatabase.deleteAllBackupRows() {
    assistantChatHistoryQueries.deleteAll()
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
    follicleQueries.deleteAll()
    embryoTransferQueries.deleteAll()
    icsiQueries.deleteAll()
    customReminderQueries.deleteAll()
    dictationCaptureQueries.deleteAll()
}

internal fun AnimallyDatabase.insertAssistantChatHistory(payload: BackupPayload) {
    payload.assistantChatHistory.forEach { row ->
        assistantChatHistoryQueries.insertWithId(
            id = row.id,
            question = row.question,
            answer = row.answer,
            source = row.source,
            interrupted = row.interrupted,
            createdAt = row.createdAt,
            conversationId = row.conversationId.ifBlank { "legacy-${row.id}" },
        )
    }
}

internal fun AnimallyDatabase.insertDictationCaptures(payload: BackupPayload) {
    payload.dictationCaptures.forEach { row ->
        dictationCaptureQueries.insertWithId(
            id = row.id,
            transcript = row.transcript,
            audioPath = row.audioPath,
            durationMillis = row.durationMillis,
            capturedAt = row.capturedAt,
        )
    }
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
        OwnerLocation.fromNullable(row.latitude, row.longitude)?.let { location ->
            ownerQueries.setLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                id = row.id,
            )
        }
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

internal fun AnimallyDatabase.insertCustomReminders(payload: BackupPayload) {
    payload.customReminders.forEach { row ->
        customReminderQueries.insertWithId(
            id = row.id,
            patientId = row.patientId,
            title = row.title,
            dueDate = row.dueDate,
            linkedRecordType = row.linkedRecordType,
            linkedRecordId = row.linkedRecordId,
            notes = row.notes,
            isActive = row.isActive,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}
