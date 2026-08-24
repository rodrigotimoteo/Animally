package com.github.rodrigotimoteo.animally.domain.dictation.model

import kotlinx.datetime.LocalDate

/**
 * Record types the dictation pipeline can suggest. Deliberately a narrow
 * subset of [com.github.rodrigotimoteo.animally.domain.common.RecordType]:
 * only record kinds whose fields the LLM contract can populate.
 */
enum class SuggestedRecordType {
    Ultrasound,
    Weight,
    Deworming,
}

/**
 * Validation outcome for a single dictated suggestion.
 *
 * Principle: drop only structural failures (unknown type, no payload);
 * everything implausible is kept and flagged for user review.
 */
sealed interface SuggestedValidationState {
    /** Suggestion passed all validation rules unchanged. */
    data object Ok : SuggestedValidationState

    /** Suggestion was adjusted or is suspicious; [reasons] explain each issue. */
    data class Flagged(
        val reasons: List<String>,
    ) : SuggestedValidationState

    /** Structurally invalid suggestion that must not reach any save path. */
    data object Dropped : SuggestedValidationState
}

/**
 * Domain model of one voice-dictation suggestion awaiting user review.
 *
 * @property recordType The kind of record suggested.
 * @property patientName Patient name as spoken in the transcript, unresolved.
 * @property date Date of the record, already normalized by validation.
 * @property weightKg Measured weight in kilograms (Weight suggestions).
 * @property ovaryStatus Ovary status description (Ultrasound suggestions).
 * @property uterineStatus Uterine status description (Ultrasound suggestions).
 * @property follicleSizeMm Follicle size in millimeters (Ultrasound suggestions).
 * @property drugName Anthelmintic product name (Deworming suggestions).
 * @property notes Free-form notes captured from the transcript.
 * @property validation Outcome of [com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase].
 */
data class SuggestedRecord(
    val recordType: SuggestedRecordType,
    val patientName: String? = null,
    val date: LocalDate? = null,
    val weightKg: Double? = null,
    val ovaryStatus: String? = null,
    val uterineStatus: String? = null,
    val follicleSizeMm: Double? = null,
    val drugName: String? = null,
    val notes: String? = null,
    val validation: SuggestedValidationState = SuggestedValidationState.Ok,
)
