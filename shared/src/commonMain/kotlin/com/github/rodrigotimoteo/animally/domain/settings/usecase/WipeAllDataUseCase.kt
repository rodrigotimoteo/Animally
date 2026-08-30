package com.github.rodrigotimoteo.animally.domain.settings.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.settings.DatabaseWipePort
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Erases every persisted record in the app: all 23 data tables plus both halves
 * of the FTS search index. The final [ISearchRepository.rebuild] re-seeds the
 * FTS index from the now-empty metadata table, leaving it consistent with the
 * wiped database.
 *
 * Irreversible by design; callers must confirm with the user first.
 *
 * @param databaseWipePort the database wipe boundary.
 * @param dictationFilePort the dictation audio file boundary.
 * @param searchRepository the global-search index to reset.
 */
@Single
class WipeAllDataUseCase(
    @Provided private val databaseWipePort: DatabaseWipePort,
    @Provided private val dictationFilePort: DictationFilePort,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Deletes every row from every data table and clears the search index.
     *
     * @throws Exception when any delete fails; the transaction rolls back and
     *  the database and its audio files keep their prior contents.
     */
    operator fun invoke() {
        val audioPaths = databaseWipePort.clearAll()
        audioPaths.forEach { path ->
            runCatching { dictationFilePort.delete(path) }
        }
        // Re-seed from the emptied metadata table so the index provably
        // matches the empty database.
        searchRepository.rebuild()
    }
}
