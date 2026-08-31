@file:Suppress("MaxLineLength", "ArgumentListWrapping", "Wrapping")

package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.backup.BACKUP_SCHEMA_VERSION
import com.github.rodrigotimoteo.animally.domain.export.CsvFormatter
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportBundle
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportDataDictionary
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportFileNames
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportMetadata
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Dashboard-aligned export bundle use case.
 *
 * Produces CSV files (one per section) plus a data dictionary in both JSON and markdown,
 * all byte-for-byte derived from the same [InsightsSnapshot] that the dashboard renders.
 * Snapshot totals therefore equal export totals for counts and averages. Pseudonymous mode
 * (default) replaces raw patient identifiers with bundle-local `P001` codes assigned by
 * ascending patientId; raw names never appear in pseudonymized CSVs. Metadata records
 * range, scope, generation time, app/schema version and definitions reference.
 *
 * Reproduction and gestation logic remains owned by Tasks 10/11; this use case only
 * formats the already-computed snapshot deterministically and handles pseudonymization,
 * CSV escaping via [CsvFormatter] and dictionary rendering.
 *
 * @param clock provider for default generation time; injectable for tests.
 */
@Suppress("TooManyFunctions")
@Single
class ExportInsightsBundleUseCase(
    private val clock: () -> Instant = { Clock.System.now() },
) {
    /**
     * Builds the bundle for [snapshot] captured for [today].
     *
     * All CSVs are derived from [snapshot] without re-querying. Current-care rows
     * are pseudonymized bundle-locally when [pseudonymize] is true. Record refs are
     * header-only unless [recordRefs] are supplied.
     *
     * @param snapshot validated snapshot from [GetInsightsDashboardUseCase] (includes repro + gestation).
     * @param today captured today for metadata context.
     * @param pseudonymize when true, raw patientIds/names are replaced by P001 pseudonyms.
     * @param generatedAt generation instant; defaults to [clock].
     * @param appVersion version string recorded in metadata.
     * @param schemaVersion schema version recorded in metadata.
     * @param recordRefs optional drill-down refs matching the filter; pseudonymized when [pseudonymize].
     * @return bundle with files, dictionary and pseudonym map.
     */
    @Suppress("LongParameterList")
    operator fun invoke(
        snapshot: InsightsSnapshot,
        today: LocalDate,
        pseudonymize: Boolean = true,
        generatedAt: Instant = clock(),
        appVersion: String = DEFAULT_APP_VERSION,
        schemaVersion: Int = BACKUP_SCHEMA_VERSION,
        recordRefs: List<InsightsRecordRef> = emptyList(),
    ): InsightsExportBundle {
        val pseudonymMap = buildPseudonymMap(snapshot, recordRefs, pseudonymize)
        val metadata =
            InsightsExportMetadata(
                generatedAt = generatedAt,
                today = today,
                rangeFrom = snapshot.appliedFilter?.from,
                rangeTo = snapshot.appliedFilter?.to,
                patientScope = snapshot.appliedFilter?.patientId,
                scopePseudonym = pseudonymMap[snapshot.appliedFilter?.patientId],
                appVersion = appVersion,
                schemaVersion = schemaVersion,
            )
        val overviewCsv = buildOverviewCsv(snapshot)
        val activitySeriesCsv = buildActivitySeriesCsv(snapshot)
        val recordMixCsv = buildRecordMixCsv(snapshot)
        val reproductionEventsCsv = buildReproductionEventsCsv(snapshot)
        val reproductionSummaryCsv = buildReproductionSummaryCsv(snapshot)
        val currentCareCsv = buildCurrentCareCsv(snapshot.currentCare.activeGestations, pseudonymMap, pseudonymize)
        val recordRefsCsv = buildRecordRefsCsv(recordRefs, pseudonymMap, pseudonymize)
        val metadataCsv = buildMetadataCsv(metadata, snapshot, pseudonymize)
        val dataDictionaryJson = buildDataDictionaryJson()
        val dataDictionaryMarkdown = buildDataDictionaryMarkdown()
        val files =
            mapOf(
                InsightsExportFileNames.OVERVIEW to overviewCsv,
                InsightsExportFileNames.ACTIVITY_SERIES to activitySeriesCsv,
                InsightsExportFileNames.RECORD_MIX to recordMixCsv,
                InsightsExportFileNames.REPRODUCTION_EVENTS to reproductionEventsCsv,
                InsightsExportFileNames.REPRODUCTION_SUMMARY to reproductionSummaryCsv,
                InsightsExportFileNames.CURRENT_CARE to currentCareCsv,
                InsightsExportFileNames.RECORD_REFS to recordRefsCsv,
                InsightsExportFileNames.METADATA to metadataCsv,
                InsightsExportFileNames.DATA_DICTIONARY_JSON to dataDictionaryJson,
                InsightsExportFileNames.DATA_DICTIONARY_MD to dataDictionaryMarkdown,
            )
        return InsightsExportBundle(
            metadata = metadata,
            overviewCsv = overviewCsv,
            activitySeriesCsv = activitySeriesCsv,
            recordMixCsv = recordMixCsv,
            reproductionEventsCsv = reproductionEventsCsv,
            reproductionSummaryCsv = reproductionSummaryCsv,
            currentCareCsv = currentCareCsv,
            recordRefsCsv = recordRefsCsv,
            metadataCsv = metadataCsv,
            dataDictionaryJson = dataDictionaryJson,
            dataDictionaryMarkdown = dataDictionaryMarkdown,
            pseudonymMap = pseudonymMap,
            files = files,
        )
    }

    private fun buildPseudonymMap(
        snapshot: InsightsSnapshot,
        recordRefs: List<InsightsRecordRef>,
        pseudonymize: Boolean,
    ): Map<Long, String> {
        if (!pseudonymize) return emptyMap()
        val ids = mutableSetOf<Long>()
        snapshot.currentCare.activeGestations.forEach { ids.add(it.patientId) }
        snapshot.appliedFilter?.patientId?.let { ids.add(it) }
        recordRefs.forEach { ids.add(it.patientId) }
        val sorted = ids.sorted()
        return sorted
            .mapIndexed { index, pid ->
                pid to pseudonym(index)
            }.toMap()
    }

    private fun pseudonym(index: Int): String {
        val number = (index + 1).toString().padStart(PSEUDONYM_PAD, '0')
        return "$PSEUDONYM_PREFIX$number"
    }

    private fun buildOverviewCsv(snapshot: InsightsSnapshot): String {
        val overview = snapshot.overview
        val comp = overview.comparison
        val headers =
            listOf(
                "patient_count",
                "activity_count",
                "case_day_count",
                "active_day_count",
                "average_per_active_day",
                "average_per_case_day",
                "comparison_current",
                "comparison_previous",
                "comparison_absolute_delta",
                "comparison_percentage_delta",
            )
        val row =
            listOf(
                overview.patientCount.toString(),
                overview.activityCount.toString(),
                overview.caseDayCount.toString(),
                overview.activeDayCount.toString(),
                formatDouble(overview.averagePerActiveDay),
                formatDouble(overview.averagePerCaseDay),
                comp?.current?.toString() ?: "",
                comp?.previous?.toString() ?: "",
                comp?.absoluteDelta?.toString() ?: "",
                formatDouble(comp?.percentageDelta),
            )
        return csvWithHeader(headers, listOf(row))
    }

    private fun buildActivitySeriesCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("period_start", "count")
        val rows =
            snapshot.activitySeries.map { point ->
                listOf(point.periodStart.toString(), point.count.toString())
            }
        return csvWithHeader(headers, rows)
    }

    private fun buildRecordMixCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("record_type_wire", "record_type_display", "count", "share")
        val rows =
            snapshot.recordMix.map { entry ->
                listOf(entry.type.wireName, entry.type.displayName, entry.count.toString(), formatDouble(entry.share))
            }
        return csvWithHeader(headers, rows)
    }

    private fun buildReproductionEventsCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("event_type_storage", "event_type_display", "count")
        val rows =
            snapshot.reproduction.eventCounts.map { entry ->
                listOf(entry.type.storageLabel, entry.type.displayLabel, entry.count.toString())
            }
        return csvWithHeader(headers, rows)
    }

    private fun buildReproductionSummaryCsv(snapshot: InsightsSnapshot): String {
        val rep = snapshot.reproduction
        val headers =
            listOf(
                "embryo_collections",
                "embryos_collected",
                "average_embryos_per_collection",
                "icsi_sessions",
                "follicles_recovered",
                "average_follicles_per_icsi",
                "ultrasound_count",
            )
        val row =
            listOf(
                rep.embryoCollections.toString(),
                rep.embryosCollected.toString(),
                formatDouble(rep.averageEmbryosPerCollection),
                rep.icsiSessions.toString(),
                rep.folliclesRecovered.toString(),
                formatDouble(rep.averageFolliclesPerIcsi),
                rep.ultrasoundCount.toString(),
            )
        return csvWithHeader(headers, listOf(row))
    }

    @Suppress("LongMethod")
    private fun buildCurrentCareCsv(
        items: List<CurrentGestationItem>,
        pseudonymMap: Map<Long, String>,
        pseudonymize: Boolean,
    ): String {
        if (pseudonymize) {
            val headers =
                listOf(
                    "patient_pseudonym",
                    "gestation_id",
                    "gestation_day",
                    "due_date",
                    "days_until_due",
                    "status",
                    "is_due_soon_30",
                    "is_due_soon_60",
                    "is_due_soon_90",
                )
            val rows =
                items.sortedBy { it.dueDate }.map { item ->
                    val pseudo = pseudonymMap[item.patientId] ?: pseudonym(0)
                    listOf(
                        pseudo,
                        item.gestationId.toString(),
                        item.gestationDay.toString(),
                        item.dueDate.toString(),
                        item.daysUntilDue.toString(),
                        item.status,
                        flag(item.daysUntilDue in DUE_SOON_30),
                        flag(item.daysUntilDue in DUE_SOON_60),
                        flag(item.daysUntilDue in DUE_SOON_90),
                    )
                }
            return csvWithHeader(headers, rows)
        } else {
            val headers =
                listOf(
                    "patient_id",
                    "patient_name",
                    "gestation_id",
                    "gestation_day",
                    "due_date",
                    "days_until_due",
                    "status",
                    "is_due_soon_30",
                    "is_due_soon_60",
                    "is_due_soon_90",
                )
            val rows =
                items.sortedBy { it.dueDate }.map { item ->
                    listOf(
                        item.patientId.toString(),
                        item.patientName,
                        item.gestationId.toString(),
                        item.gestationDay.toString(),
                        item.dueDate.toString(),
                        item.daysUntilDue.toString(),
                        item.status,
                        flag(item.daysUntilDue in DUE_SOON_30),
                        flag(item.daysUntilDue in DUE_SOON_60),
                        flag(item.daysUntilDue in DUE_SOON_90),
                    )
                }
            return csvWithHeader(headers, rows)
        }
    }

    private fun buildRecordRefsCsv(
        refs: List<InsightsRecordRef>,
        pseudonymMap: Map<Long, String>,
        pseudonymize: Boolean,
    ): String {
        if (pseudonymize) {
            val headers = listOf("patient_pseudonym", "record_type_wire", "record_id", "date")
            val rows =
                refs.sortedWith(compareByDescending<InsightsRecordRef> { it.date }.thenByDescending { it.recordId }).map { ref ->
                    val pseudo = pseudonymMap[ref.patientId] ?: pseudonym(0)
                    listOf(pseudo, ref.recordType.wireName, ref.recordId.toString(), ref.date.toString())
                }
            return csvWithHeader(headers, rows)
        } else {
            val headers = listOf("patient_id", "patient_name", "record_type_wire", "record_id", "date")
            val rows =
                refs.sortedWith(compareByDescending<InsightsRecordRef> { it.date }.thenByDescending { it.recordId }).map { ref ->
                    listOf(ref.patientId.toString(), ref.patientName, ref.recordType.wireName, ref.recordId.toString(), ref.date.toString())
                }
            return csvWithHeader(headers, rows)
        }
    }

    private fun buildMetadataCsv(
        metadata: InsightsExportMetadata,
        snapshot: InsightsSnapshot,
        pseudonymize: Boolean,
    ): String {
        val headers = listOf("key", "value")
        val scopeValue =
            if (pseudonymize) {
                metadata.scopePseudonym ?: (metadata.patientScope?.toString() ?: "all")
            } else {
                metadata.patientScope?.toString() ?: "all"
            }
        val rows =
            mutableListOf(
                listOf("generated_at", metadata.generatedAt.toString()),
                listOf("today", metadata.today.toString()),
                listOf("range_from", metadata.rangeFrom?.toString() ?: ""),
                listOf("range_to", metadata.rangeTo?.toString() ?: ""),
                listOf("patient_scope", scopeValue),
                listOf("app_version", metadata.appVersion),
                listOf("schema_version", metadata.schemaVersion.toString()),
                listOf("activity_count", snapshot.overview.activityCount.toString()),
                listOf("patient_count", snapshot.overview.patientCount.toString()),
                listOf("generation_note", "Export derived byte-for-byte from the same InsightsSnapshot as the dashboard; totals match."),
                listOf("definitions", "See data_dictionary.json and data_dictionary.md for column definitions."),
            )
        return csvWithHeader(headers, rows)
    }

    @Suppress("MaxLineLength")
    private fun buildDataDictionaryJson(): String {
        val tables = InsightsExportDataDictionary.tables
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"generated_note\": \"Column definitions for the Insights export bundle. See plan.md Exact Metric Semantics.\",\n")
        sb.append("  \"tables\": [\n")
        tables.forEachIndexed { ti, table ->
            sb.append("    {\n")
            sb.append("      \"file\": \"${jsonEscape(table.file)}\",\n")
            sb.append("      \"description\": \"${jsonEscape(table.description)}\",\n")
            sb.append("      \"columns\": [\n")
            table.columns.forEachIndexed { ci, col ->
                sb.append(
                    "        {\"name\": \"${jsonEscape(col.name)}\", \"type\": \"${jsonEscape(col.type)}\", \"definition\": \"${jsonEscape(col.definition)}\"}",
                )
                if (ci < table.columns.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("      ]\n")
            sb.append("    }")
            if (ti < tables.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun buildDataDictionaryMarkdown(): String {
        val sb = StringBuilder()
        sb.append("# Insights Export Data Dictionary\n\n")
        sb.append("Generated from the same `InsightsSnapshot` as the dashboard; all totals match the on-screen values.\n\n")
        sb.append("Ranges are inclusive (`from <= date <= to`). Zero denominators yield empty values, not zero or NaN. ")
        sb.append("Current-care rows are as of the captured `today`, independent of the historical range. ")
        sb.append("Pseudonymized mode replaces `patientId`/`patientName` with bundle-local `P001` codes assigned by ascending patientId.\n\n")
        sb.append("## Files\n\n")
        InsightsExportDataDictionary.tables.forEach { table ->
            sb.append("### `${table.file}`\n\n")
            sb.append("${table.description}\n\n")
            sb.append("| Column | Type | Definition |\n")
            sb.append("|--------|------|------------|\n")
            table.columns.forEach { col ->
                sb.append("| `${col.name}` | ${col.type} | ${mdEscape(col.definition)} |\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun csvWithHeader(
        headers: List<String>,
        rows: List<List<Any?>>,
    ): String {
        val sb = StringBuilder()
        sb.append(CsvFormatter.line(headers))
        rows.forEach { row -> sb.append(CsvFormatter.line(row)) }
        return sb.toString()
    }

    private fun formatDouble(value: Double?): String = value?.toString() ?: ""

    private fun flag(condition: Boolean): String = if (condition) "1" else "0"

    private fun jsonEscape(raw: String): String =
        raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun mdEscape(raw: String): String = raw.replace("|", "\\|").replace("\n", " ")

    private companion object {
        const val DEFAULT_APP_VERSION = "1.0.0"
        const val PSEUDONYM_PAD = 3
        const val PSEUDONYM_PREFIX = "P"
        val DUE_SOON_30: IntRange = 0..30
        val DUE_SOON_60: IntRange = 0..60
        val DUE_SOON_90: IntRange = 0..90
    }
}
