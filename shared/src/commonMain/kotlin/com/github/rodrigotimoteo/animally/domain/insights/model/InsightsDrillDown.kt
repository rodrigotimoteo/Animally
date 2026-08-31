package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate

/**
 * Filter for drill-down from a dashboard metric to its source rows.
 *
 * Ranges are inclusive and validated before repository access. Record type is
 * selected by tapping a case-mix segment and applies only to the drill-down
 * list in the first milestone.
 *
 * @property from inclusive lower bound.
 * @property to inclusive upper bound, never before [from].
 * @property patientId optional patient scope; null means global.
 * @property recordType optional record-type filter; null means all types.
 * @property reproductionEventType optional canonical reproduction subtype.
 */
data class InsightsDrillDown(
    val from: LocalDate,
    val to: LocalDate,
    val patientId: Long? = null,
    val recordType: RecordType? = null,
    val reproductionEventType: ReproductionEventType? = null,
    val dataIssueType: InsightsDataIssueType? = null,
) {
    init {
        require(from <= to) {
            "InsightsDrillDown requires from <= to, got from=$from to=$to"
        }
        require(reproductionEventType == null || recordType == RecordType.ReproductionEvent) {
            "A reproduction event subtype requires the Reproduction record type"
        }
        require(dataIssueType == null || reproductionEventType == null) {
            "Data-issue drill-down cannot combine reproductionEventType and dataIssueType"
        }
    }
}
