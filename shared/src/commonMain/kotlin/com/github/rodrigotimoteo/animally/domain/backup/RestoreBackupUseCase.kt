package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Replaces the current database contents with the records of a previously
 * exported [BackupPayload].
 *
 * The restore runs inside one transaction: every table is cleared and then
 * re-populated preserving ids, active flags and timestamps, so cross-table
 * references (e.g. `patientId`) stay intact. The derived FTS search index is
 * not touched because its rows keep pointing at the same, re-inserted ids.
 *
 * @param database the database to restore into.
 */
@Single
class RestoreBackupUseCase(
    @Provided private val database: AnimallyDatabase,
) {
    /**
     * Decodes [jsonContent] and replaces all database rows with its contents.
     *
     * @throws IllegalArgumentException when the payload schema version is unsupported.
     */
    operator fun invoke(jsonContent: String) {
        val payload = BackupSerializer.decode(jsonContent)
        database.transaction {
            database.deleteAllBackupRows()
            database.insertOwners(payload)
            database.insertPatients(payload)
            database.insertAnamnese(payload)
            database.insertConsultations(payload)
            database.insertVaccinations(payload)
            database.insertWeights(payload)
            database.insertDewormings(payload)
            database.insertDentistry(payload)
            database.insertLameness(payload)
            database.insertSurgeries(payload)
            database.insertMedications(payload)
            database.insertLabResults(payload)
            database.insertImaging(payload)
            database.insertFarrierVisits(payload)
            database.insertReproductionEvents(payload)
            database.insertUltrasounds(payload)
            database.insertFollicles(payload)
            database.insertGestations(payload)
            database.insertReproMedications(payload)
            database.insertSubstances(payload)
            database.insertEmbryoTransfers(payload)
            database.insertIcsi(payload)
        }
    }
}
