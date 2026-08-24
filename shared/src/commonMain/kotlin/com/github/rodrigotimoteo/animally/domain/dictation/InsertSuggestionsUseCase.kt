package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.SaveDewormingUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecordType
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.SaveUltrasoundUseCase
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.SaveWeightUseCase
import kotlin.time.Clock

/**
 * One accepted suggestion paired with the patient it should be inserted for.
 *
 * @property record The validated, user-accepted suggestion.
 * @property patientId Resolved target patient.
 */
data class SuggestedInsertion(
    val record: SuggestedRecord,
    val patientId: Long,
)

/**
 * Per-record outcome of an insertion batch, in input order.
 */
sealed interface InsertionResult {
    /** The suggestion was persisted; [id] is the generated record identifier. */
    data class Inserted(
        val recordType: SuggestedRecordType,
        val id: Long,
    ) : InsertionResult

    /** The suggestion failed to persist; [message] describes the failure. */
    data class Failed(
        val recordType: SuggestedRecordType,
        val message: String,
    ) : InsertionResult
}

/**
 * Persists accepted dictation suggestions through the existing save use cases.
 *
 * Insertions run sequentially in input order; a failure on one record does not
 * abort the remaining ones.
 *
 * @param saveUltrasoundUseCase Save path for ultrasound records.
 * @param saveWeightUseCase Save path for weight entries.
 * @param saveDewormingUseCase Save path for deworming records.
 */
class InsertSuggestionsUseCase(
    private val saveUltrasoundUseCase: SaveUltrasoundUseCase,
    private val saveWeightUseCase: SaveWeightUseCase,
    private val saveDewormingUseCase: SaveDewormingUseCase,
) {
    /**
     * Inserts every [insertions] entry for its resolved patient.
     *
     * @param insertions Accepted suggestions with their resolved patient ids.
     * @return one [InsertionResult] per input, in input order.
     */
    operator fun invoke(insertions: List<SuggestedInsertion>): List<InsertionResult> =
        insertions.map { insertion ->
            runCatching { insertOne(insertion) }
                .fold(
                    onSuccess = { id -> InsertionResult.Inserted(insertion.record.recordType, id) },
                    onFailure = { error ->
                        InsertionResult.Failed(insertion.record.recordType, error.message ?: "unknown error")
                    },
                )
        }

    private fun insertOne(insertion: SuggestedInsertion): Long =
        when (insertion.record.recordType) {
            SuggestedRecordType.Ultrasound -> insertUltrasound(insertion)
            SuggestedRecordType.Weight -> insertWeight(insertion)
            SuggestedRecordType.Deworming -> insertDeworming(insertion)
        }

    private fun insertUltrasound(insertion: SuggestedInsertion): Long {
        val record = insertion.record
        return saveUltrasoundUseCase(
            Ultrasound(
                id = 0L,
                patientId = insertion.patientId,
                date = requireDate(record),
                ovaryStatus = record.ovaryStatus,
                uterineStatus = record.uterineStatus,
                follicleSizeMm = record.follicleSizeMm,
                notes = record.notes,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
        )
    }

    private fun insertWeight(insertion: SuggestedInsertion): Long {
        val record = insertion.record
        return saveWeightUseCase(
            Weight(
                id = 0L,
                patientId = insertion.patientId,
                weightKg = requireNotNull(record.weightKg) { "weight suggestion without weightKg" },
                date = requireDate(record),
                notes = record.notes,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
        )
    }

    private fun insertDeworming(insertion: SuggestedInsertion): Long {
        val record = insertion.record
        return saveDewormingUseCase(
            Deworming(
                id = 0L,
                patientId = insertion.patientId,
                product = requireNotNull(record.drugName) { "deworming suggestion without drugName" },
                dateAdministered = requireDate(record),
                notes = record.notes,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
        )
    }

    private fun requireDate(record: SuggestedRecord) = checkNotNull(record.date) { "suggestion without date" }
}
