@file:Suppress("TooManyFunctions")

package com.github.rodrigotimoteo.animally.data.insights.mapper

import com.github.rodrigotimoteo.animally.data.insights.SelectActivityBucketsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectActivityBucketsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectActivityRecordRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectActivityRecordRefsForPatientByType
import com.github.rodrigotimoteo.animally.data.insights.SelectActivityRecordRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectActivityRecordRefsGlobalByType
import com.github.rodrigotimoteo.animally.data.insights.SelectEmbryoStatsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectEmbryoStatsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectFreeTextEmbryoRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectFreeTextEmbryoRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectIcsiStatsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectIcsiStatsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectIncompleteUltrasoundRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectIncompleteUltrasoundRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectMissingVetRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectMissingVetRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectReproductionEventCountsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectReproductionEventCountsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectReproductionRecordRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectReproductionRecordRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectUnknownReproductionRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectUnknownReproductionRefsGlobal
import com.github.rodrigotimoteo.animally.data.insights.SelectUnlinkedOwnerRefsForPatient
import com.github.rodrigotimoteo.animally.data.insights.SelectUnlinkedOwnerRefsGlobal
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionEventCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType

/**
 * Mapper for Insights SQLDelight rows to domain facts.
 * Preserves soft-delete and active-patient semantics enforced in SQL.
 * Unknown wire names are filtered out rather than crashing; stable [RecordType.wireName] literals
 * in SQL guarantee parse succeeds for valid rows.
 * M6 split: both scoped and global variants share same projection → duplicate generated types.
 * SQLDelight generates distinct classes per query (SelectActivityBucketsForPatient vs SelectActivityBucketsGlobal
 * etc.) even when projections are identical, so this mapper provides thin delegating extensions plus
 * shared helpers [mapBucket] / [mapRecordRef] to reduce duplication.
 */
private fun mapBucket(
    date: kotlinx.datetime.LocalDate,
    patientId: Long,
    recordTypeWireName: String,
    cnt: Long,
): InsightsActivityBucket? {
    val type = RecordType.fromWireName(recordTypeWireName) ?: return null
    return InsightsActivityBucket(date = date, patientId = patientId, recordType = type, count = cnt.toInt())
}

private fun mapRecordRef(
    recordTypeWireName: String,
    patientId: Long,
    recordId: Long,
    patientName: String,
    date: kotlinx.datetime.LocalDate,
): InsightsRecordRef? {
    val type = RecordType.fromWireName(recordTypeWireName) ?: return null
    return InsightsRecordRef(recordType = type, patientId = patientId, recordId = recordId, patientName = patientName, date = date)
}

internal fun SelectActivityBucketsForPatient.toDomain(): InsightsActivityBucket? = mapBucket(date, patientId, recordTypeWireName, cnt)

internal fun SelectActivityBucketsGlobal.toDomain(): InsightsActivityBucket? = mapBucket(date, patientId, recordTypeWireName, cnt)

internal fun SelectActivityRecordRefsForPatient.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectActivityRecordRefsForPatientByType.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectActivityRecordRefsGlobal.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectActivityRecordRefsGlobalByType.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectReproductionRecordRefsForPatient.toDomain(requiredType: ReproductionEventType): InsightsRecordRef? {
    if (ReproductionEventType.from(eventType) != requiredType) return null
    return InsightsRecordRef(
        recordType = RecordType.ReproductionEvent,
        patientId = patientId,
        recordId = recordId,
        patientName = patientName,
        date = date,
    )
}

internal fun SelectReproductionRecordRefsGlobal.toDomain(requiredType: ReproductionEventType): InsightsRecordRef? {
    if (ReproductionEventType.from(eventType) != requiredType) return null
    return InsightsRecordRef(
        recordType = RecordType.ReproductionEvent,
        patientId = patientId,
        recordId = recordId,
        patientName = patientName,
        date = date,
    )
}

/**
 * Canonicalised reproduction event counts — tolerant to legacy spellings and unknown -> Other.
 *
 * SQL groups by raw `eventType` string; Kotlin merges by [ReproductionEventType.from].
 */
internal fun List<SelectReproductionEventCountsForPatient>.toReproductionEventCountsForPatient(): List<ReproductionEventCount> {
    if (isEmpty()) return emptyList()
    return groupBy { ReproductionEventType.from(it.eventType) }
        .map { (type, rows) -> ReproductionEventCount(type = type, count = rows.sumOf { it.cnt.toInt() }) }
        .sortedWith(compareByDescending<ReproductionEventCount> { it.count }.thenBy { it.type.storageLabel })
}

internal fun List<SelectReproductionEventCountsGlobal>.toReproductionEventCountsGlobal(): List<ReproductionEventCount> {
    if (isEmpty()) return emptyList()
    return groupBy { ReproductionEventType.from(it.eventType) }
        .map { (type, rows) -> ReproductionEventCount(type = type, count = rows.sumOf { it.cnt.toInt() }) }
        .sortedWith(compareByDescending<ReproductionEventCount> { it.count }.thenBy { it.type.storageLabel })
}

internal fun SelectEmbryoStatsForPatient.toDomain(): Pair<Int, Int> = collections.toInt() to totalEmbryos.toInt()

internal fun SelectEmbryoStatsGlobal.toDomain(): Pair<Int, Int> = collections.toInt() to totalEmbryos.toInt()

internal fun SelectIcsiStatsForPatient.toDomain(): Pair<Int, Int> = sessions.toInt() to totalFollicles.toInt()

internal fun SelectIcsiStatsGlobal.toDomain(): Pair<Int, Int> = sessions.toInt() to totalFollicles.toInt()

// --- Task 13: readiness drill-down mappers ---

internal fun SelectUnknownReproductionRefsForPatient.toDomainFiltered(): InsightsRecordRef? {
    if (ReproductionEventType.from(eventType) != ReproductionEventType.Other) return null
    return InsightsRecordRef(
        recordType = RecordType.ReproductionEvent,
        patientId = patientId,
        recordId = recordId,
        patientName = patientName,
        date = date,
    )
}

internal fun SelectUnknownReproductionRefsGlobal.toDomainFiltered(): InsightsRecordRef? {
    if (ReproductionEventType.from(eventType) != ReproductionEventType.Other) return null
    return InsightsRecordRef(
        recordType = RecordType.ReproductionEvent,
        patientId = patientId,
        recordId = recordId,
        patientName = patientName,
        date = date,
    )
}

internal fun SelectFreeTextEmbryoRefsForPatient.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectFreeTextEmbryoRefsGlobal.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectIncompleteUltrasoundRefsForPatient.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectIncompleteUltrasoundRefsGlobal.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectMissingVetRefsForPatient.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectMissingVetRefsGlobal.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectUnlinkedOwnerRefsForPatient.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

internal fun SelectUnlinkedOwnerRefsGlobal.toDomain(): InsightsRecordRef? = mapRecordRef(recordTypeWireName, patientId, recordId, patientName, date)

/**
 * Builds [ReproductionMetrics] from already-aggregated facts.
 * Averages are null when denominators zero; no success rates.
 */
@Suppress("LongParameterList")
internal fun buildReproductionMetrics(
    eventCounts: List<ReproductionEventCount>,
    embryoCollections: Int,
    embryosCollected: Int,
    icsiSessions: Int,
    folliclesRecovered: Int,
    ultrasoundCount: Int,
): ReproductionMetrics =
    ReproductionMetrics(
        eventCounts = eventCounts,
        embryoCollections = embryoCollections,
        embryosCollected = embryosCollected,
        averageEmbryosPerCollection = ReproductionMetrics.avgEmbryos(embryosCollected, embryoCollections),
        icsiSessions = icsiSessions,
        folliclesRecovered = folliclesRecovered,
        averageFolliclesPerIcsi = ReproductionMetrics.avgFollicles(folliclesRecovered, icsiSessions),
        ultrasoundCount = ultrasoundCount,
    )
