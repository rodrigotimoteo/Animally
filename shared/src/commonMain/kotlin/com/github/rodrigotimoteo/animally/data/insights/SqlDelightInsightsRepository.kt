package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.gestation.SelectActiveGestationsForPatient
import com.github.rodrigotimoteo.animally.data.gestation.SelectActiveGestationsGlobal
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single(binds = [IInsightsRepository::class])
class SqlDelightInsightsRepository(
    @Provided private val database: AnimallyDatabase,
) : IInsightsRepository {
    private val reproductionReader = SqlDelightInsightsReproductionReader(database)
    private val dataIssueReader = SqlDelightInsightsDataIssueReader(database)

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
    ): InsightsRecordRef? =
        RecordType.fromWireName(wire)?.let {
            InsightsRecordRef(it, patientId, recordId, patientName, date)
        }

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
        drillDown.dataIssueType?.let {
            return getDataIssueRecordRefs(InsightsFilter(drillDown.from, drillDown.to, drillDown.patientId), it)
        }
        drillDown.reproductionEventType?.let {
            return getReproductionEventRefs(drillDown)
        }
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
                    .map {
                        InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date)
                    }
            },
            {
                database.insightsQueries
                    .selectReproductionRecordRefsGlobalByNormalized(drillDown.from, drillDown.to, normalized)
                    .executeAsList()
                    .map {
                        InsightsRecordRef(RecordType.ReproductionEvent, it.patientId, it.recordId, it.patientName, it.date)
                    }
            },
        )
    }

    override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics = reproductionReader.read(filter)

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
                        .map { row -> row.toGestationValues().toCurrentCareItem(today) }
                },
                {
                    database.gestationQueries
                        .selectActiveGestationsGlobal()
                        .executeAsList()
                        .map { row -> row.toGestationValues().toCurrentCareItem(today) }
                },
            )
        return CurrentCareSnapshot(items)
    }

    override fun getDataIssueCounts(filter: InsightsFilter) = dataIssueReader.counts(filter)

    override fun getDataIssueRecordRefs(
        filter: InsightsFilter,
        issueType: InsightsDataIssueType,
    ): List<InsightsRecordRef> = dataIssueReader.recordRefs(filter, issueType)
}

private data class GestationValues(
    val patientId: Long,
    val patientName: String,
    val gestationId: Long,
    val breedingDate: LocalDate,
    val expectedDueDate: LocalDate,
    val status: String?,
)

private fun SelectActiveGestationsForPatient.toGestationValues() = GestationValues(patientId, patientName, gestationId, breedingDate, expectedDueDate, status)

private fun SelectActiveGestationsGlobal.toGestationValues() = GestationValues(patientId, patientName, gestationId, breedingDate, expectedDueDate, status)

private fun GestationValues.toCurrentCareItem(today: LocalDate): CurrentGestationItem =
    CurrentGestationItem(
        patientId,
        patientName,
        gestationId,
        breedingDate.daysUntil(today).coerceAtLeast(0),
        expectedDueDate,
        today.daysUntil(expectedDueDate),
        status ?: "",
    )
