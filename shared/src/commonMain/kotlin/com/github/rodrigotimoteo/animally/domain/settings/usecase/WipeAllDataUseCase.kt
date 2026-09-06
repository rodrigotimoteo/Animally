package com.github.rodrigotimoteo.animally.domain.settings.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.settings.DatabaseWipePort
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Erases every persisted record in the app: all 25 data tables plus both halves
 * of the FTS search index. The final [ISearchRepository.rebuild] re-seeds the
 * FTS index from the now-empty metadata table, leaving it consistent with the
 * wiped database. It also removes app-owned audio and clinical attachment
 * paths returned by the database boundary.
 *
 * Irreversible by design; callers must confirm with the user first.
 *
 * @param databaseWipePort the database wipe boundary.
 * @param dictationFilePort the app-owned media file boundary.
 * @param searchRepository the global-search index to reset.
 * @param reminderScheduler the platform reminder scheduler to clear.
 */
@Single
class WipeAllDataUseCase(
    @Provided private val databaseWipePort: DatabaseWipePort,
    @Provided private val dictationFilePort: DictationFilePort,
    @Provided private val searchRepository: ISearchRepository,
    @Provided private val reminderScheduler: ReminderScheduler,
) {
    /** Outcome of local erase, including cleanup that can fail outside SQLite. */
    data class Result(
        val residualMediaPaths: Set<String>,
        val notificationsCancelled: Boolean,
        val searchIndexRebuilt: Boolean,
    ) {
        /** Whether all local cleanup obligations completed. */
        val isComplete: Boolean
            get() = residualMediaPaths.isEmpty() && notificationsCancelled && searchIndexRebuilt
    }

    /**
     * Deletes every row from every data table, clears the search index, and
     * removes referenced app-owned media while reporting any residual paths.
     *
     * Database deletion is committed before external media cleanup, so a
     * failed file or notification operation is represented in [Result] rather
     * than reported as an all-or-nothing database failure.
     */
    operator fun invoke(): Result {
        val mediaPaths = databaseWipePort.clearAll()
        val residualMediaPaths =
            mediaPaths.filterTo(mutableSetOf()) { path ->
                !runCatching { dictationFilePort.delete(path) }.getOrDefault(false)
            }
        val notificationsCancelled = runCatching { reminderScheduler.cancelAll() }.isSuccess
        // Re-seed from the emptied metadata table so the index provably
        // matches the empty database.
        val searchIndexRebuilt = runCatching { searchRepository.rebuild() }.isSuccess
        return Result(residualMediaPaths, notificationsCancelled, searchIndexRebuilt)
    }
}
