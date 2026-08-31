package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Metadata recorded alongside the exported bundle for audit and reproducibility.
 *
 * Contains generation time, filter range, scope, version info and definitions
 * reference; stored as both a CSV row set and embedded in the data dictionary.
 *
 * @property generatedAt instant the bundle was produced.
 * @property today captured today used for current-care snapshot.
 * @property rangeFrom inclusive lower bound from [InsightsSnapshot.appliedFilter], null for empty all-time.
 * @property rangeTo inclusive upper bound from [InsightsSnapshot.appliedFilter], null for empty all-time.
 * @property patientScope raw patient scope from [InsightsSnapshot.appliedFilter], null means all patients.
 * @property scopePseudonym bundle-local pseudonym for [patientScope] when pseudonymized.
 * @property appVersion application version string included for reproducibility.
 * @property schemaVersion schema version for the export shape.
 */
data class InsightsExportMetadata(
    val generatedAt: Instant,
    val today: LocalDate,
    val rangeFrom: LocalDate?,
    val rangeTo: LocalDate?,
    val patientScope: Long?,
    val scopePseudonym: String?,
    val appVersion: String,
    val schemaVersion: Int,
)

/**
 * Dashboard-aligned export bundle produced from a validated [InsightsSnapshot].
 *
 * Every file is byte-for-byte derived from the same [InsightsSnapshot] values that
 * the dashboard renders, so snapshot totals equal export totals. Pseudonymous mode
 * replaces raw patient identifiers with bundle-local `P001` codes assigned by
 * ascending patientId; raw names never appear in pseudonymized CSVs.
 *
 * @property metadata generation context and version info.
 * @property overviewCsv single-row overview metrics.
 * @property activitySeriesCsv trend buckets (period_start, count).
 * @property recordMixCsv record-type distribution.
 * @property reproductionEventsCsv canonical reproduction event counts.
 * @property reproductionSummaryCsv embryo/ICSI/ultrasound aggregates.
 * @property currentCareCsv active gestations with pseudonymous patient column.
 * @property recordRefsCsv header-only placeholder for drill-down refs (no rows unless supplied).
 * @property metadataCsv key/value export context.
 * @property dataDictionaryJson JSON form of column definitions.
 * @property dataDictionaryMarkdown markdown form of column definitions.
 * @property pseudonymMap bundle-local `patientId -> PNNN` when pseudonymized.
 * @property files filename to content map for writing to disk/share sheet.
 */
data class InsightsExportBundle(
    val metadata: InsightsExportMetadata,
    val overviewCsv: String,
    val activitySeriesCsv: String,
    val recordMixCsv: String,
    val reproductionEventsCsv: String,
    val reproductionSummaryCsv: String,
    val currentCareCsv: String,
    val recordRefsCsv: String,
    val metadataCsv: String,
    val dataDictionaryJson: String,
    val dataDictionaryMarkdown: String,
    val pseudonymMap: Map<Long, String>,
    val files: Map<String, String>,
)

/**
 * Stable filenames for the bundle. Adding a file here requires updating
 * [com.github.rodrigotimoteo.animally.domain.insights.model.DataDictionary] and tests.
 */
object InsightsExportFileNames {
    const val OVERVIEW = "overview.csv"
    const val ACTIVITY_SERIES = "activity_series.csv"
    const val RECORD_MIX = "record_mix.csv"
    const val REPRODUCTION_EVENTS = "reproduction_events.csv"
    const val REPRODUCTION_SUMMARY = "reproduction_summary.csv"
    const val CURRENT_CARE = "current_care.csv"
    const val RECORD_REFS = "record_refs.csv"
    const val METADATA = "metadata.csv"
    const val DATA_DICTIONARY_JSON = "data_dictionary.json"
    const val DATA_DICTIONARY_MD = "data_dictionary.md"
}
