@file:Suppress("MaxLineLength", "ArgumentListWrapping")

package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.serialization.Serializable

@Serializable
data class InsightsExportColumnDef(
    val name: String,
    val type: String,
    val definition: String,
)

@Serializable
data class InsightsExportTableDef(
    val file: String,
    val description: String,
    val columns: List<InsightsExportColumnDef>,
)

/**
 * Static data dictionary shared by both JSON and markdown renderings.
 *
 * Every exported file appears here with exact column names, logical types and
 * definitions aligned with `plan.md` Exact Metric Semantics. Updating the export
 * shape requires updating this dictionary and the corresponding CSV builders.
 */
object InsightsExportDataDictionary {
    val tables: List<InsightsExportTableDef> =
        listOf(
            InsightsExportTableDef(
                file = InsightsExportFileNames.OVERVIEW,
                description = "Single-row overview metrics for the applied filter; matches dashboard header cards byte-for-byte.",
                columns =
                    listOf(
                        InsightsExportColumnDef("patient_count", "INTEGER", "Distinct patients with at least one active dated record in range."),
                        InsightsExportColumnDef("activity_count", "INTEGER", "Count of active dated records in range (one row = one activity)."),
                        InsightsExportColumnDef("case_day_count", "INTEGER", "Distinct (patientId, date) pairs with at least one activity."),
                        InsightsExportColumnDef("active_day_count", "INTEGER", "Distinct dates with at least one activity."),
                        InsightsExportColumnDef("average_per_active_day", "DOUBLE", "activity_count / active_day_count, empty when denominator zero."),
                        InsightsExportColumnDef("average_per_case_day", "DOUBLE", "activity_count / case_day_count, empty when denominator zero."),
                        InsightsExportColumnDef("comparison_current", "INTEGER", "activity_count in current period."),
                        InsightsExportColumnDef("comparison_previous", "INTEGER", "activity_count in preceding equal-length period, empty when all-time."),
                        InsightsExportColumnDef("comparison_absolute_delta", "INTEGER", "current - previous, empty when all-time."),
                        InsightsExportColumnDef("comparison_percentage_delta", "DOUBLE", "100 * delta / previous, empty when previous zero or all-time."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.ACTIVITY_SERIES,
                description = "Activity trend buckets; granularity daily ≤45d, weekly 46..180d, monthly >180d (calendar boundaries).",
                columns =
                    listOf(
                        InsightsExportColumnDef("period_start", "DATE", "Inclusive start of bucket (date, ISO Monday, or month 01). ISO-8601 LocalDate."),
                        InsightsExportColumnDef("count", "INTEGER", "Activity count in bucket."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.RECORD_MIX,
                description = "Record-type distribution for the period; counts sum to overview activity_count.",
                columns =
                    listOf(
                        InsightsExportColumnDef("record_type_wire", "STRING", "Stable wireName for RecordType (e.g., CONSULTATION)."),
                        InsightsExportColumnDef("record_type_display", "STRING", "Human display name for RecordType."),
                        InsightsExportColumnDef("count", "INTEGER", "Number of activities of this type."),
                        InsightsExportColumnDef("share", "DOUBLE", "count / activity_count, empty when total zero."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.REPRODUCTION_EVENTS,
                description = "Canonical reproduction event counts; legacy spellings collapsed, unknown -> Other raw value preserved.",
                columns =
                    listOf(
                        InsightsExportColumnDef(
                            "event_type_storage",
                            "STRING",
                            "Canonical storageLabel (Heat, Breeding, Pregnancy Check, Foaling, Initial Exam, Other).",
                        ),
                        InsightsExportColumnDef("event_type_display", "STRING", "Human displayLabel matching storageLabel."),
                        InsightsExportColumnDef("count", "INTEGER", "Number of events of this canonical type in range."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.REPRODUCTION_SUMMARY,
                description = "Embryo, ICSI and ultrasound aggregates for the period; averages empty when denominator zero. No success rates.",
                columns =
                    listOf(
                        InsightsExportColumnDef("embryo_collections", "INTEGER", "Count of EmbryoTransfer rows in range."),
                        InsightsExportColumnDef("embryos_collected", "INTEGER", "SUM(embryoCount) across collections."),
                        InsightsExportColumnDef("average_embryos_per_collection", "DOUBLE", "embryos_collected / embryo_collections, empty when zero."),
                        InsightsExportColumnDef("icsi_sessions", "INTEGER", "Count of Icsi rows in range."),
                        InsightsExportColumnDef("follicles_recovered", "INTEGER", "SUM(folliclesRecovered) across ICSI sessions."),
                        InsightsExportColumnDef("average_follicles_per_icsi", "DOUBLE", "follicles_recovered / icsi_sessions, empty when zero."),
                        InsightsExportColumnDef("ultrasound_count", "INTEGER", "Count of Ultrasound rows in range."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.CURRENT_CARE,
                description = "Current operational gestation snapshot as of today, independent of historical range; pseudonymous when enabled.",
                columns =
                    listOf(
                        InsightsExportColumnDef(
                            "patient_pseudonym",
                            "STRING",
                            "Bundle-local pseudonym P001, P002 assigned ascending patientId; replaces raw id/name.",
                        ),
                        InsightsExportColumnDef("gestation_id", "INTEGER", "Source Gestation row identifier for navigation."),
                        InsightsExportColumnDef("gestation_day", "INTEGER", "Days since breedingDate as of today, recalculated not trusted."),
                        InsightsExportColumnDef("due_date", "DATE", "Persisted expectedDueDate (ISO-8601)."),
                        InsightsExportColumnDef("days_until_due", "INTEGER", "today.daysUntil(dueDate); negative when overdue."),
                        InsightsExportColumnDef("status", "STRING", "Gestation status label as stored."),
                        InsightsExportColumnDef("is_due_soon_30", "INTEGER", "1 when 0 <= days_until_due <= 30 else 0."),
                        InsightsExportColumnDef("is_due_soon_60", "INTEGER", "1 when 0 <= days_until_due <= 60 else 0."),
                        InsightsExportColumnDef("is_due_soon_90", "INTEGER", "1 when 0 <= days_until_due <= 90 else 0."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.RECORD_REFS,
                description = "Drill-down record references matching the applied filter; pseudonymous when enabled, ordered date desc.",
                columns =
                    listOf(
                        InsightsExportColumnDef("patient_pseudonym", "STRING", "Bundle-local pseudonym; empty when global aggregate with no rows."),
                        InsightsExportColumnDef("record_type_wire", "STRING", "RecordType wireName."),
                        InsightsExportColumnDef("record_id", "INTEGER", "Source row id for navigation."),
                        InsightsExportColumnDef("date", "DATE", "Record date used for filtering and ordering."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.METADATA,
                description = "Export context and reproducibility info: range, scope, generation time, versions and metric definitions reference.",
                columns =
                    listOf(
                        InsightsExportColumnDef("key", "STRING", "Metadata field name."),
                        InsightsExportColumnDef("value", "STRING", "Field value."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.DATA_DICTIONARY_JSON,
                description = "Machine-readable column definitions for every exported file.",
                columns =
                    listOf(
                        InsightsExportColumnDef("file", "STRING", "Export filename."),
                        InsightsExportColumnDef("column", "STRING", "Column name."),
                        InsightsExportColumnDef("type", "STRING", "Logical type."),
                        InsightsExportColumnDef("definition", "STRING", "Meaning and derivation."),
                    ),
            ),
            InsightsExportTableDef(
                file = InsightsExportFileNames.DATA_DICTIONARY_MD,
                description = "Human-readable markdown column definitions for every exported file.",
                columns =
                    listOf(
                        InsightsExportColumnDef("file", "STRING", "Export filename."),
                        InsightsExportColumnDef("column", "STRING", "Column name."),
                        InsightsExportColumnDef("type", "STRING", "Logical type."),
                        InsightsExportColumnDef("definition", "STRING", "Meaning and derivation."),
                    ),
            ),
        )
}
