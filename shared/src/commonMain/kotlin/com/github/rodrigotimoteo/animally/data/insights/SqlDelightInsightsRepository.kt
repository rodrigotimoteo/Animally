@file:Suppress("LongMethod", "CyclomaticComplexMethod", "TooManyFunctions", "Wrapping", "MaximumLineLength", "MaxLineLength", "LongParameterList")

package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionEventCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.insights.model.avgOrNull
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single(binds = [IInsightsRepository::class])
class SqlDelightInsightsRepository(
    @Provided private val database: AnimallyDatabase,
) : IInsightsRepository {
    private inline fun <T> withScope(
        patientId: Long?,
        scoped: (Long) -> T,
        global: () -> T,
    ): T = if (patientId == null) global() else scoped(patientId)

    private fun mapRecordRef(
        wire: String,
        patientId: Long,
        recordId: Long,
        patientName: String,
        date: LocalDate,
    ): InsightsRecordRef? = RecordType.fromWireName(wire)?.let { InsightsRecordRef(it, patientId, recordId, patientName, date) }

    private fun toCurrentGestationItem(
        patientId: Long,
        patientName: String,
        gestationId: Long,
        breedingDate: LocalDate,
        expectedDueDate: LocalDate,
        status: String?,
        today: LocalDate,
    ): CurrentGestationItem =
        CurrentGestationItem(
            patientId,
            patientName,
            gestationId,
            breedingDate.daysUntil(today).coerceAtLeast(0),
            expectedDueDate,
            today.daysUntil(expectedDueDate),
            status ?: "",
        )

    override fun getEarliestActivityDate(patientId: Long?): LocalDate? =
        withScope(
            patientId,
            { database.insightsQueries.selectEarliestActivityDateForPatient(it).executeAsOneOrNull() },
            { database.insightsQueries.selectEarliestActivityDateGlobal().executeAsOneOrNull() },
        )

    override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> {
        val buckets =
            withScope(
                filter.patientId,
                { pid ->
                    database.insightsQueries
                        .selectActivityBucketsForPatient(filter.from, filter.to, pid)
                        .executeAsList()
                        .mapNotNull {
                            RecordType.fromWireName(it.recordTypeWireName)?.let { t ->
                                InsightsActivityBucket(it.date, it.patientId, t, it.cnt.toInt())
                            }
                        }
                },
                {
                    database.insightsQueries
                        .selectActivityBucketsGlobal(filter.from, filter.to)
                        .executeAsList()
                        .mapNotNull {
                            RecordType.fromWireName(it.recordTypeWireName)?.let { t ->
                                InsightsActivityBucket(it.date, it.patientId, t, it.cnt.toInt())
                            }
                        }
                },
            )
        return buckets
    }

    override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> {
        drillDown.dataIssueType?.let { return getDataIssueRecordRefs(InsightsFilter(drillDown.from, drillDown.to, drillDown.patientId), it) }
        drillDown.reproductionEventType?.let { return getReproductionEventRefs(drillDown) }
        val wire = drillDown.recordType?.wireName
        return withScope(
            drillDown.patientId,
            { pid ->
                database.insightsQueries
                    .selectActivityRecordRefsForPatient(drillDown.from, drillDown.to, pid, wire)
                    .executeAsList()
                    .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
            },
            {
                database.insightsQueries
                    .selectActivityRecordRefsGlobal(drillDown.from, drillDown.to, wire)
                    .executeAsList()
                    .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
            },
        )
    }

    private fun getReproductionEventRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> {
        val requiredType = requireNotNull(drillDown.reproductionEventType)
        val normalized = ReproductionEventType.normalize(requiredType.storageLabel)
        return withScope(
            drillDown.patientId,
            { pid ->
                database.insightsQueries
                    .selectReproductionRecordRefsForPatientByNormalized(drillDown.from, drillDown.to, pid, normalized)
                    .executeAsList()
                    .map { InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date) }
            },
            {
                database.insightsQueries
                    .selectReproductionRecordRefsGlobalByNormalized(drillDown.from, drillDown.to, normalized)
                    .executeAsList()
                    .map { InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date) }
            },
        )
    }

    override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics {
        val rows: List<Pair<String, Long>> =
            withScope(
                filter.patientId,
                { pid ->
                    database.insightsQueries
                        .selectReproductionEventCountsForPatient(filter.from, filter.to, pid)
                        .executeAsList()
                        .map { it.eventType to it.cnt }
                },
                {
                    database.insightsQueries
                        .selectReproductionEventCountsGlobal(filter.from, filter.to)
                        .executeAsList()
                        .map { it.eventType to it.cnt }
                },
            )
        val eventCounts =
            rows
                .groupBy { ReproductionEventType.from(it.first) }
                .map { (type, list) -> ReproductionEventCount(type, list.sumOf { it.second.toInt() }) }
                .sortedWith(compareByDescending<ReproductionEventCount> { it.count }.thenBy { it.type.storageLabel })

        val (embryoCollections, embryosCollected) =
            withScope(
                filter.patientId,
                { pid ->
                    database.insightsQueries.selectEmbryoStatsForPatient(filter.from, filter.to, pid).executeAsOne().let {
                        it.collections.toInt() to
                            it.totalEmbryos.toInt()
                    }
                },
                {
                    database.insightsQueries
                        .selectEmbryoStatsGlobal(
                            filter.from,
                            filter.to,
                        ).executeAsOne()
                        .let { it.collections.toInt() to it.totalEmbryos.toInt() }
                },
            )
        val (icsiSessions, folliclesRecovered) =
            withScope(
                filter.patientId,
                { pid ->
                    database.insightsQueries.selectIcsiStatsForPatient(filter.from, filter.to, pid).executeAsOne().let {
                        it.sessions.toInt() to
                            it.totalFollicles.toInt()
                    }
                },
                {
                    database.insightsQueries
                        .selectIcsiStatsGlobal(filter.from, filter.to)
                        .executeAsOne()
                        .let { it.sessions.toInt() to it.totalFollicles.toInt() }
                },
            )
        val ultrasoundCount =
            withScope(
                filter.patientId,
                { pid ->
                    database.insightsQueries
                        .selectUltrasoundCountForPatient(filter.from, filter.to, pid)
                        .executeAsOne()
                        .toInt()
                },
                {
                    database.insightsQueries
                        .selectUltrasoundCountGlobal(filter.from, filter.to)
                        .executeAsOne()
                        .toInt()
                },
            )
        return ReproductionMetrics(
            eventCounts = eventCounts,
            embryoCollections = embryoCollections,
            embryosCollected = embryosCollected,
            averageEmbryosPerCollection = avgOrNull(embryosCollected, embryoCollections),
            icsiSessions = icsiSessions,
            folliclesRecovered = folliclesRecovered,
            averageFolliclesPerIcsi = avgOrNull(folliclesRecovered, icsiSessions),
            ultrasoundCount = ultrasoundCount,
        )
    }

    override fun getCurrentCareSnapshot(
        patientId: Long?,
        today: LocalDate,
    ): CurrentCareSnapshot {
        val items =
            withScope(
                patientId,
                { pid ->
                    database.gestationQueries
                        .selectActiveGestationsForPatient(pid)
                        .executeAsList()
                        .map { toCurrentGestationItem(it.patientId, it.patientName, it.gestationId, it.breedingDate, it.expectedDueDate, it.status, today) }
                },
                {
                    database.gestationQueries
                        .selectActiveGestationsGlobal()
                        .executeAsList()
                        .map { toCurrentGestationItem(it.patientId, it.patientName, it.gestationId, it.breedingDate, it.expectedDueDate, it.status, today) }
                },
            )
        return CurrentCareSnapshot(items)
    }

    override fun getDataIssueCounts(filter: InsightsFilter): List<InsightsDataIssueCount> =
        listOfNotNull(
            countOrNull(
                InsightsDataIssueType.UnknownReproductionCategory,
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectUnknownReproductionCountForPatient(filter.from, filter.to, pid)
                            .executeAsOne()
                            .toInt()
                    },
                    {
                        database.insightsQueries
                            .selectUnknownReproductionCountGlobal(filter.from, filter.to)
                            .executeAsOne()
                            .toInt()
                    },
                ),
            ),
            countOrNull(
                InsightsDataIssueType.MissingVetName,
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectMissingVetCountForPatient(filter.from, filter.to, pid)
                            .executeAsOne()
                            .toInt()
                    },
                    {
                        database.insightsQueries
                            .selectMissingVetCountGlobal(filter.from, filter.to)
                            .executeAsOne()
                            .toInt()
                    },
                ),
            ),
            countOrNull(
                InsightsDataIssueType.UnlinkedOwner,
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectUnlinkedOwnerCountForPatient(filter.from, filter.to, pid)
                            .executeAsOne()
                            .toInt()
                    },
                    {
                        database.insightsQueries
                            .selectUnlinkedOwnerCountGlobal(filter.from, filter.to)
                            .executeAsOne()
                            .toInt()
                    },
                ),
            ),
            countOrNull(
                InsightsDataIssueType.FreeTextEmbryoRecipient,
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectFreeTextEmbryoCountForPatient(filter.from, filter.to, pid)
                            .executeAsOne()
                            .toInt()
                    },
                    {
                        database.insightsQueries
                            .selectFreeTextEmbryoCountGlobal(filter.from, filter.to)
                            .executeAsOne()
                            .toInt()
                    },
                ),
            ),
            countOrNull(
                InsightsDataIssueType.IncompleteUltrasoundData,
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectIncompleteUltrasoundCountForPatient(filter.from, filter.to, pid)
                            .executeAsOne()
                            .toInt()
                    },
                    {
                        database.insightsQueries
                            .selectIncompleteUltrasoundCountGlobal(filter.from, filter.to)
                            .executeAsOne()
                            .toInt()
                    },
                ),
            ),
        )

    private fun countOrNull(
        type: InsightsDataIssueType,
        count: Int,
    ): InsightsDataIssueCount? = if (count > 0) InsightsDataIssueCount(type, count) else null

    override fun getDataIssueRecordRefs(
        filter: InsightsFilter,
        issueType: InsightsDataIssueType,
    ): List<InsightsRecordRef> =
        when (issueType) {
            InsightsDataIssueType.UnknownReproductionCategory ->
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectUnknownReproductionRefsForPatient(filter.from, filter.to, pid)
                            .executeAsList()
                            .map { InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                    {
                        database.insightsQueries
                            .selectUnknownReproductionRefsGlobal(filter.from, filter.to)
                            .executeAsList()
                            .map { InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                )
            InsightsDataIssueType.MissingVetName ->
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectMissingVetRefsForPatient(filter.from, filter.to, pid)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                    {
                        database.insightsQueries
                            .selectMissingVetRefsGlobal(filter.from, filter.to)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                )
            InsightsDataIssueType.UnlinkedOwner ->
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectUnlinkedOwnerRefsForPatient(filter.from, filter.to, pid)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                    {
                        database.insightsQueries
                            .selectUnlinkedOwnerRefsGlobal(filter.from, filter.to)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                )
            InsightsDataIssueType.FreeTextEmbryoRecipient ->
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectFreeTextEmbryoRefsForPatient(filter.from, filter.to, pid)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                    {
                        database.insightsQueries
                            .selectFreeTextEmbryoRefsGlobal(filter.from, filter.to)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                )
            InsightsDataIssueType.IncompleteUltrasoundData ->
                withScope(
                    filter.patientId,
                    { pid ->
                        database.insightsQueries
                            .selectIncompleteUltrasoundRefsForPatient(filter.from, filter.to, pid)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                    {
                        database.insightsQueries
                            .selectIncompleteUltrasoundRefsGlobal(filter.from, filter.to)
                            .executeAsList()
                            .mapNotNull { mapRecordRef(it.recordTypeWireName, it.patientId, it.recordId, it.patientName, it.date) }
                    },
                )
        }
}
