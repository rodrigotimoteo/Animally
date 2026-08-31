@file:Suppress("MaxLineLength", "ArgumentListWrapping", "Wrapping", "PropertyWrapping", "MaximumLineLength", "ConstructorParameterNaming")

package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.backup.BACKUP_SCHEMA_VERSION
import com.github.rodrigotimoteo.animally.domain.export.CsvFormatter
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportBundle
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportDataDictionary
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportFileNames
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportMetadata
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportTableDef
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
private data class DictionaryJson(
    val generated_note: String,
    val tables: List<InsightsExportTableDef>,
)

@Suppress("TooManyFunctions", "LongMethod")
@Single
class ExportInsightsBundleUseCase(
    private val clock: () -> Instant = { Clock.System.now() },
) {
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
                generatedAt,
                today,
                snapshot.appliedFilter?.from,
                snapshot.appliedFilter?.to,
                snapshot.appliedFilter?.patientId,
                pseudonymMap[snapshot.appliedFilter?.patientId],
                appVersion,
                schemaVersion,
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
            metadata,
            overviewCsv,
            activitySeriesCsv,
            recordMixCsv,
            reproductionEventsCsv,
            reproductionSummaryCsv,
            currentCareCsv,
            recordRefsCsv,
            metadataCsv,
            dataDictionaryJson,
            dataDictionaryMarkdown,
            pseudonymMap,
            files,
        )
    }

    private fun buildPseudonymMap(
        snapshot: InsightsSnapshot,
        refs: List<InsightsRecordRef>,
        pseudonymize: Boolean,
    ): Map<Long, String> {
        if (!pseudonymize) return emptyMap()
        val ids =
            buildSet {
                snapshot.currentCare.activeGestations.forEach { add(it.patientId) }
                snapshot.appliedFilter?.patientId?.let { add(it) }
                refs.forEach { add(it.patientId) }
            }.sorted()
        return ids.mapIndexed { i, pid -> pid to pseudonym(i) }.toMap()
    }

    private fun pseudonym(index: Int): String = "P${(index + 1).toString().padStart(PSEUDONYM_PAD, '0')}"

    private fun patientColumns(pseudonymize: Boolean): List<String> = if (pseudonymize) listOf("patient_pseudonym") else listOf("patient_id", "patient_name")

    private fun patientCells(
        pseudonymize: Boolean,
        patientId: Long,
        patientName: String,
        map: Map<Long, String>,
    ): List<String> = if (pseudonymize) listOf(map[patientId] ?: FALLBACK_PSEUDONYM) else listOf(patientId.toString(), patientName)

    private fun buildOverviewCsv(snapshot: InsightsSnapshot): String {
        val o = snapshot.overview
        val c = o.comparison
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
                o.patientCount,
                o.activityCount,
                o.caseDayCount,
                o.activeDayCount,
                formatDouble(o.averagePerActiveDay),
                formatDouble(o.averagePerCaseDay),
                c?.current ?: "",
                c?.previous ?: "",
                c?.absoluteDelta ?: "",
                formatDouble(c?.percentageDelta),
            )
        return csv(headers, listOf(row))
    }

    private fun buildActivitySeriesCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("period_start", "count")
        val rows = snapshot.activitySeries.map { listOf(it.periodStart.toString(), it.count.toString()) }
        return csv(headers, rows)
    }

    private fun buildRecordMixCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("record_type_wire", "record_type_display", "count", "share")
        val rows = snapshot.recordMix.map { listOf(it.type.wireName, it.type.displayName, it.count.toString(), formatDouble(it.share)) }
        return csv(headers, rows)
    }

    private fun buildReproductionEventsCsv(snapshot: InsightsSnapshot): String {
        val headers = listOf("event_type_storage", "event_type_display", "count")
        val rows = snapshot.reproduction.eventCounts.map { listOf(it.type.storageLabel, it.type.displayLabel, it.count.toString()) }
        return csv(headers, rows)
    }

    private fun buildReproductionSummaryCsv(snapshot: InsightsSnapshot): String {
        val r = snapshot.reproduction
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
                r.embryoCollections,
                r.embryosCollected,
                formatDouble(r.averageEmbryosPerCollection),
                r.icsiSessions,
                r.folliclesRecovered,
                formatDouble(r.averageFolliclesPerIcsi),
                r.ultrasoundCount,
            )
        return csv(headers, listOf(row))
    }

    private fun buildCurrentCareCsv(
        items: List<CurrentGestationItem>,
        pseudonymMap: Map<Long, String>,
        pseudonymize: Boolean,
    ): String {
        val headers =
            patientColumns(pseudonymize) +
                listOf(
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
                patientCells(pseudonymize, item.patientId, item.patientName, pseudonymMap) +
                    listOf(
                        item.gestationId.toString(),
                        item.gestationDay.toString(),
                        item.dueDate.toString(),
                        item.daysUntilDue.toString(),
                        item.status,
                        flag(item.isDueSoon(CurrentCareSnapshot.DUE_SOON_30_DAYS)),
                        flag(item.isDueSoon(CurrentCareSnapshot.DUE_SOON_60_DAYS)),
                        flag(item.isDueSoon(CurrentCareSnapshot.DUE_SOON_90_DAYS)),
                    )
            }
        return csv(headers, rows)
    }

    private fun buildRecordRefsCsv(
        refs: List<InsightsRecordRef>,
        pseudonymMap: Map<Long, String>,
        pseudonymize: Boolean,
    ): String {
        val headers = patientColumns(pseudonymize) + listOf("record_type_wire", "record_id", "date")
        val rows =
            refs.sortedWith(compareByDescending<InsightsRecordRef> { it.date }.thenByDescending { it.recordId }).map { ref ->
                patientCells(pseudonymize, ref.patientId, ref.patientName, pseudonymMap) +
                    listOf(ref.recordType.wireName, ref.recordId.toString(), ref.date.toString())
            }
        return csv(headers, rows)
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
                metadata.patientScope?.toString()
                    ?: "all"
            }
        val rows =
            listOf(
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
        return csv(headers, rows)
    }

    private fun buildDataDictionaryJson(): String =
        json.encodeToString(
            DictionaryJson("Column definitions for the Insights export bundle. See plan.md Exact Metric Semantics.", InsightsExportDataDictionary.tables),
        )

    private fun buildDataDictionaryMarkdown(): String =
        buildString {
            append(
                "# Insights Export Data Dictionary\n\nGenerated from the same `InsightsSnapshot` as the dashboard; all totals match the on-screen values.\n\n",
            )
            append(
                "Ranges are inclusive (`from <= date <= to`). Zero denominators yield empty values, not zero or NaN. Current-care rows are as of the captured `today`, independent of the historical range. Pseudonymized mode replaces `patientId`/`patientName` with bundle-local `P001` codes assigned by ascending patientId.\n\n## Files\n\n",
            )
            InsightsExportDataDictionary.tables.forEach { table ->
                append("### `${table.file}`\n\n${table.description}\n\n| Column | Type | Definition |\n|--------|------|------------|\n")
                table.columns.forEach { col -> append("| `${col.name}` | ${col.type} | ${col.definition.replace("|","\\|").replace("\n"," ")} |\n") }
                append("\n")
            }
        }

    private fun csv(
        headers: List<String>,
        rows: List<List<Any?>>,
    ): String {
        val sb = StringBuilder()
        sb.append(CsvFormatter.line(headers))
        rows.forEach { sb.append(CsvFormatter.line(it)) }
        return sb.toString()
    }

    private fun formatDouble(v: Double?): String = v?.toString() ?: ""

    private fun flag(c: Boolean): String = if (c) "1" else "0"

    private companion object {
        const val DEFAULT_APP_VERSION = "1.0.0"
        const val PSEUDONYM_PAD = 3
        const val FALLBACK_PSEUDONYM = "P000"
        val json: Json = Json { prettyPrint = true }
    }
}
