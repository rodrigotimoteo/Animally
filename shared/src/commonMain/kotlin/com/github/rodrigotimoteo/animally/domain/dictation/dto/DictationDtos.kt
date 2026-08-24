package com.github.rodrigotimoteo.animally.domain.dictation.dto

import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecordType
import kotlinx.serialization.Serializable

/**
 * Wire contract for a full dictated session, produced by the on-device LLM.
 *
 * Mirrors the Swift `DictatedSession` JSON contract exactly; field names and
 * nullability must not change without updating the Swift side.
 */
@Serializable
data class DictatedSessionDto(
    val records: List<SuggestedRecordDto> = emptyList(),
)

/**
 * Wire contract for one suggested record inside a [DictatedSessionDto].
 *
 * Every field except [recordType] is optional and defaults to null: the LLM
 * fills only what the transcript expressed. Unknown extra keys are tolerated
 * by the decoder so the Swift contract can evolve independently.
 *
 * @property recordType One of "ultrasound", "weight", "deworming" (case-insensitive).
 * @property patientName Patient name as spoken, unresolved.
 * @property date ISO-8601 local date (`yyyy-MM-dd`), or free text when unparseable.
 * @property weightKg Measured weight in kilograms.
 * @property ovaryStatus Ovary status description.
 * @property uterineStatus Uterine status description.
 * @property follicleSizeMm Follicle size in millimeters.
 * @property drugName Anthelmintic product name.
 * @property notes Free-form notes from the transcript.
 */
@Serializable
data class SuggestedRecordDto(
    val recordType: String,
    val patientName: String? = null,
    val date: String? = null,
    val weightKg: Double? = null,
    val ovaryStatus: String? = null,
    val uterineStatus: String? = null,
    val follicleSizeMm: Double? = null,
    val drugName: String? = null,
    val notes: String? = null,
)

/**
 * Maps this DTO to its domain representation.
 *
 * @return the domain suggestion, or `null` when [recordType] is unknown —
 *   a structural failure the validator turns into a dropped suggestion.
 */
fun SuggestedRecordDto.toSuggestedRecord(): SuggestedRecord? {
    val type =
        SuggestedRecordType.entries.firstOrNull {
            it.name.equals(recordType, ignoreCase = true)
        } ?: return null
    return SuggestedRecord(
        recordType = type,
        patientName = patientName,
        weightKg = weightKg,
        ovaryStatus = ovaryStatus,
        uterineStatus = uterineStatus,
        follicleSizeMm = follicleSizeMm,
        drugName = drugName,
        notes = notes,
    )
}
