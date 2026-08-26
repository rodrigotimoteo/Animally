package com.github.rodrigotimoteo.animally.data.dictation

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** SQLDelight implementation of the local dictation archive. */
@Single(binds = [IDictationCaptureRepository::class])
class DictationCaptureRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
    @Provided private val queries: DictationCaptureQueries,
) : IDictationCaptureRepository {
    override fun getAll(): List<DictationCapture> =
        queries
            .selectAll()
            .executeAsList()
            .map { row ->
                DictationCapture(
                    id = row.id,
                    transcript = row.transcript,
                    audioPath = row.audioPath,
                    durationMillis = row.durationMillis,
                    capturedAt = row.capturedAt,
                )
            }

    override fun insert(capture: DictationCapture): Long =
        database.transactionWithResult {
            queries.insert(
                transcript = capture.transcript,
                audioPath = capture.audioPath,
                durationMillis = capture.durationMillis,
                capturedAt = capture.capturedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun getById(id: Long): DictationCapture? =
        queries.selectById(id).executeAsOneOrNull()?.let { row ->
            DictationCapture(
                id = row.id,
                transcript = row.transcript,
                audioPath = row.audioPath,
                durationMillis = row.durationMillis,
                capturedAt = row.capturedAt,
            )
        }

    override fun deleteById(id: Long): Long = queries.deleteById(id).value

    override fun deleteAll() {
        queries.deleteAll().value
    }
}
