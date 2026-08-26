package com.github.rodrigotimoteo.animally.domain.settings.usecase

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.backup.deleteAllBackupRows
import com.github.rodrigotimoteo.animally.domain.backup.deleteDictationAudioFiles
import com.github.rodrigotimoteo.animally.domain.backup.dictationAudioPaths
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Erases every persisted record in the app: all 23 data tables (the exact set
 * [deleteAllBackupRows] covers, shared with backup/restore) plus both halves
 * of the FTS search index. The final [ISearchRepository.rebuild] re-seeds the
 * FTS index from the now-empty metadata table, leaving it consistent with the
 * wiped database.
 *
 * Irreversible by design; callers must confirm with the user first.
 *
 * @param database the database to wipe.
 * @param searchRepository the global-search index to reset.
 */
@Single
class WipeAllDataUseCase(
    @Provided private val database: AnimallyDatabase,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Deletes every row from every data table and clears the search index.
     *
     * @throws Exception when any delete fails; the transaction rolls back and
     *  the database and its audio files keep their prior contents.
     */
    operator fun invoke() {
        val audioPaths = database.dictationAudioPaths()
        database.transaction {
            database.deleteAllBackupRows()
            database.searchFtsQueries.deleteAllIndex()
            database.searchFtsQueries.deleteAllFts()
        }
        audioPaths.deleteDictationAudioFiles()
        // Re-seed from the emptied metadata table so the index provably
        // matches the empty database.
        searchRepository.rebuild()
    }
}
