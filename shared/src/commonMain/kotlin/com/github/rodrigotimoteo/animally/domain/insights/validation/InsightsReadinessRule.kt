package com.github.rodrigotimoteo.animally.domain.insights.validation

import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType

/**
 * Research-readiness validation rules for the Insights dashboard (Task 13).
 *
 * Each rule has an explicit definition surfaced as "missing for analysis"
 * rather than "clinically wrong", and each drills down to the exact affected
 * source rows. No opaque combined quality score is used.
 *
 * Rules mirror Task 13 acceptance:
 * - Unknown reproduction categories
 * - Missing veterinarian names
 * - Unlinked owners
 * - Free-text embryo recipients
 * - Incomplete structured ultrasound data
 *
 * All rules respect shared filters: inclusive date range, patient scope,
 * `isActive = 1` and `Patient.isActive = 1` where applicable. Medication
 * additionally requires `startDate IS NOT NULL`.
 */
object InsightsReadinessRule {
    /**
     * Returns true when a raw reproduction eventType should be counted as
     * [InsightsDataIssueType.UnknownReproductionCategory].
     *
     * Tolerant to legacy spellings (case, spaces, underscores, hyphens).
     * Unknown values map to [ReproductionEventType.Other].
     */
    fun isUnknownReproductionCategory(rawEventType: String): Boolean = ReproductionEventType.from(rawEventType) == ReproductionEventType.Other

    /**
     * Returns true when a vet attribution string is considered missing.
     * Blank after trim (null, empty, whitespace-only) → missing for analysis.
     */
    fun isMissingVetName(vetName: String?): Boolean = vetName == null || vetName.trim().isEmpty()

    /**
     * Returns true when a patient has no linked owner (ownerId == null).
     * The affected rows for analysis are activity records whose patient has
     * `ownerId IS NULL`.
     */
    fun isUnlinkedOwner(ownerId: Long?): Boolean = ownerId == null

    /**
     * Returns true when an EmbryoTransfer recipientMares field contains
     * free-text (non-blank) — flagged as missing for analysis because
     * structured linkage not captured.
     */
    fun isFreeTextEmbryoRecipient(recipientMares: String?): Boolean = recipientMares != null && recipientMares.trim().isNotEmpty()

    /**
     * Returns true when ultrasound structured fields are all missing.
     *
     * Structured fields: ovaryStatus, uterineStatus, follicleSizeMm,
     * leftOvaryStatus, rightOvaryStatus, leftFollicleSizeMm,
     * rightFollicleSizeMm, uterineEdema, uterineLiquid,
     * uterineLiquidDescription, uterusDescription.
     * If every structured field is null/blank and numeric fields null,
     * the row is incomplete for analysis even when free-text findings exist.
     */
    @Suppress("LongParameterList")
    fun isIncompleteUltrasound(
        ovaryStatus: String?,
        uterineStatus: String?,
        follicleSizeMm: Double?,
        leftOvaryStatus: String?,
        rightOvaryStatus: String?,
        leftFollicleSizeMm: Double?,
        rightFollicleSizeMm: Double?,
        uterineEdema: String?,
        uterineLiquid: Boolean?,
        uterineLiquidDescription: String?,
        uterusDescription: String?,
    ): Boolean {
        val textMissing = { v: String? -> v == null || v.trim().isEmpty() }
        return textMissing(ovaryStatus) &&
            textMissing(uterineStatus) &&
            follicleSizeMm == null &&
            textMissing(leftOvaryStatus) &&
            textMissing(rightOvaryStatus) &&
            leftFollicleSizeMm == null &&
            rightFollicleSizeMm == null &&
            textMissing(uterineEdema) &&
            uterineLiquid == null &&
            textMissing(uterineLiquidDescription) &&
            textMissing(uterusDescription)
    }

    /**
     * Human definition for [type] used in UI and export dictionary.
     * Delegates to the enum's own [InsightsDataIssueType.definition].
     */
    fun definition(type: InsightsDataIssueType): String = type.definition

    /**
     * Missing-for-analysis label for [type].
     */
    fun missingLabel(type: InsightsDataIssueType): String = type.missingForAnalysisLabel
}
