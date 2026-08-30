package com.github.rodrigotimoteo.animally.data.settings

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.settings.DatabaseWipePort
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Data-side adapter for [DatabaseWipePort].
 *
 * Captures dictation audio paths, then clears all 23 data tables plus both
 * halves of the FTS index in one transaction — identical to the former
 * `AnimallyDatabase.deleteAllBackupRows()` + FTS clearing in
 * `WipeAllDataUseCase`.
 */
@Single(binds = [DatabaseWipePort::class])
class DatabaseWipePortImpl(
    @Provided private val database: AnimallyDatabase,
) : DatabaseWipePort {
    override fun clearAll(): Set<String> {
        val audioPaths =
            database.dictationCaptureQueries
                .selectAll()
                .executeAsList()
                .mapNotNull { it.audioPath }
                .toSet()
        database.transaction {
            database.assistantChatHistoryQueries.deleteAll()
            database.anamneseQueries.deleteAll()
            database.consultationQueries.deleteAll()
            database.dentistryQueries.deleteAll()
            database.dewormingQueries.deleteAll()
            database.farrierVisitQueries.deleteAll()
            database.gestationQueries.deleteAll()
            database.imagingQueries.deleteAll()
            database.labResultQueries.deleteAll()
            database.lamenessQueries.deleteAll()
            database.medicationQueries.deleteAll()
            database.ownerQueries.deleteAll()
            database.patientQueries.deleteAll()
            database.reproMedicationQueries.deleteAll()
            database.reproductionQueries.deleteAll()
            database.substanceQueries.deleteAll()
            database.surgeryQueries.deleteAll()
            database.ultrasoundQueries.deleteAll()
            database.vaccinationQueries.deleteAll()
            database.weightQueries.deleteAll()
            database.follicleQueries.deleteAll()
            database.embryoTransferQueries.deleteAll()
            database.icsiQueries.deleteAll()
            database.customReminderQueries.deleteAll()
            database.dictationCaptureQueries.deleteAll()
            database.searchFtsQueries.deleteAllIndex()
            database.searchFtsQueries.deleteAllFts()
        }
        return audioPaths
    }
}
