package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Replaces the current database contents with the records of a previously
 * exported [BackupPayload].
 *
 * The restore runs inside one transaction: every table is cleared and then
 * re-populated preserving ids, active flags and timestamps, so cross-table
 * references (e.g. `patientId`) stay intact. The derived FTS search index is
 * cleared inside the same transaction and rebuilt from the restored records
 * afterwards, so deleted or changed records cannot remain searchable.
 *
 * @param database the database to restore into.
 * @param searchRepository the search-index maintenance boundary used to
 * rebuild derived search data after the restore.
 */
@Single
class RestoreBackupUseCase(
    @Provided private val database: AnimallyDatabase,
    @Provided private val searchRepository: ISearchRepository,
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
            // FTS rows are derived data. Clearing both tables while the source
            // rows are empty prevents stale results even if reindexing later
            // fails; the next startup will retry the versioned healing pass.
            database.searchFtsQueries.deleteAllFts().value
            database.searchFtsQueries.deleteAllIndex().value
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
            database.insertCustomReminders(payload)
        }
        searchRepository.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
    }
}
