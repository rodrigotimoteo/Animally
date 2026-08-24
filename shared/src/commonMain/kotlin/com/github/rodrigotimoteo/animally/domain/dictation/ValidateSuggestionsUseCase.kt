package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.dto.SuggestedRecordDto
import com.github.rodrigotimoteo.animally.domain.dictation.dto.toSuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Maximum plausible equine weight in kilograms; values above it are nulled.
 */
private const val WEIGHT_HARD_MAX_KG = 3000.0

/**
 * Weight above which a value is implausible but kept and flagged for review.
 */
private const val WEIGHT_SUSPECT_MIN_KG = 1500.0

/**
 * Maximum plausible follicle size in millimeters.
 */
private const val FOLLICLE_HARD_MAX_MM = 100.0

/**
 * How many days back a record date may fall before it is flagged.
 */
private const val DATE_MAX_AGE_DAYS = 365

/**
 * Maximum accepted drug name length; longer names are truncated.
 */
private const val DRUG_NAME_MAX_LENGTH = 100

/**
 * Validates dictated suggestions before they reach the review UI.
 *
 * Principle: drop only structural failures (unknown record type, no payload
 * beyond a date); everything implausible is kept and flagged so the user can
 * decide. Unparseable dates default to today and are flagged; an absent date
 * defaults to today silently.
 *
 * Validation rules:
 * - Unknown [recordType][SuggestedRecordDto.recordType] -> dropped.
 * - No payload field beyond the date -> dropped.
 * - Date present but unparseable -> today, flagged ("date_unparseable").
 * - Date absent -> today, silently (not dictated is different from garbled).
 * - Date outside `[today - 365, today]` -> kept, flagged.
 * - Weight <= 0 or > 3000 kg -> nulled, flagged; 1500-3000 kg -> kept, flagged.
 * - Follicle size <= 0 or > 100 mm -> kept, flagged.
 * - Drug name longer than 100 chars -> truncated, flagged.
 */
class ValidateSuggestionsUseCase {
    /**
     * Validates every suggestion in [records].
     *
     * @param records Raw suggestions decoded from the dictation session JSON.
     * @param today Reference date for range checks; defaults to the system date.
     * @return one validated [SuggestedRecord] per valid input, in input order.
     *   Suggestions whose record type is unknown are structural failures and
     *   are excluded entirely (the domain model only represents known types).
     */
    operator fun invoke(
        records: List<SuggestedRecordDto>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<SuggestedRecord> {
        return records.mapNotNull { dto ->
            val base = dto.toSuggestedRecord() ?: return@mapNotNull null
            validateOne(base, dto, today)
        }
    }

    private fun validateOne(
        base: SuggestedRecord,
        dto: SuggestedRecordDto,
        today: LocalDate,
    ): SuggestedRecord {
        if (hasNoPayload(dto)) return base.copy(validation = SuggestedValidationState.Dropped)

        val reasons = mutableListOf<String>()
        val date = resolveDate(dto.date, today, reasons)
        val weightKg = sanitizeWeight(dto.weightKg, reasons)
        validateFollicleSize(dto.follicleSizeMm, reasons)
        val drugName = truncateDrugName(dto.drugName, reasons)

        val validation =
            if (reasons.isEmpty()) {
                SuggestedValidationState.Ok
            } else {
                SuggestedValidationState.Flagged(reasons)
            }
        return base.copy(
            date = date,
            weightKg = weightKg,
            drugName = drugName,
            validation = validation,
        )
    }

    private fun hasNoPayload(dto: SuggestedRecordDto): Boolean =
        dto.weightKg == null &&
            dto.ovaryStatus == null &&
            dto.uterineStatus == null &&
            dto.follicleSizeMm == null &&
            dto.drugName == null &&
            dto.notes == null

    private fun resolveDate(
        raw: String?,
        today: LocalDate,
        reasons: MutableList<String>,
    ): LocalDate {
        if (raw == null) return today
        val parsed = runCatching { LocalDate.parse(raw) }.getOrNull()
        if (parsed == null) {
            reasons += REASON_DATE_UNPARSEABLE
            return today
        }
        val ageDays = parsed.daysUntil(today)
        if (ageDays < 0 || ageDays > DATE_MAX_AGE_DAYS) {
            reasons += REASON_DATE_OUT_OF_RANGE
        }
        return parsed
    }

    /** Returns [weightKg], or null plus a flag reason when implausible. */
    private fun sanitizeWeight(
        weightKg: Double?,
        reasons: MutableList<String>,
    ): Double? {
        if (weightKg == null) return null
        return when {
            weightKg <= 0.0 || weightKg > WEIGHT_HARD_MAX_KG -> {
                reasons += REASON_WEIGHT_IMPLAUSIBLE
                null
            }
            weightKg >= WEIGHT_SUSPECT_MIN_KG -> {
                reasons += REASON_WEIGHT_HIGH
                weightKg
            }
            else -> weightKg
        }
    }

    private fun validateFollicleSize(
        follicleSizeMm: Double?,
        reasons: MutableList<String>,
    ) {
        if (follicleSizeMm == null) return
        if (follicleSizeMm <= 0.0 || follicleSizeMm > FOLLICLE_HARD_MAX_MM) {
            reasons += REASON_FOLLICLE_IMPLAUSIBLE
        }
    }

    private fun truncateDrugName(
        drugName: String?,
        reasons: MutableList<String>,
    ): String? {
        if (drugName == null || drugName.length <= DRUG_NAME_MAX_LENGTH) return drugName
        reasons += REASON_DRUG_NAME_TRUNCATED
        return drugName.take(DRUG_NAME_MAX_LENGTH)
    }

    private companion object {
        const val REASON_DATE_UNPARSEABLE = "date_unparseable"
        const val REASON_DATE_OUT_OF_RANGE = "date_out_of_range"
        const val REASON_WEIGHT_IMPLAUSIBLE = "weight_implausible"
        const val REASON_WEIGHT_HIGH = "weight_high"
        const val REASON_FOLLICLE_IMPLAUSIBLE = "follicle_size_implausible"
        const val REASON_DRUG_NAME_TRUNCATED = "drug_name_truncated"
    }
}
