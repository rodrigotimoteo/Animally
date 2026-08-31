package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import kotlinx.datetime.LocalDate

internal class SqlDelightInsightsDataIssueReader(
    private val database: AnimallyDatabase,
) {
    fun counts(filter: InsightsFilter): List<InsightsDataIssueCount> =
        listOfNotNull(
            countOrNull(InsightsDataIssueType.UnknownReproductionCategory, unknownReproductionCount(filter)),
            countOrNull(InsightsDataIssueType.MissingVetName, missingVetCount(filter)),
            countOrNull(InsightsDataIssueType.UnlinkedOwner, unlinkedOwnerCount(filter)),
            countOrNull(InsightsDataIssueType.FreeTextEmbryoRecipient, freeTextEmbryoCount(filter)),
            countOrNull(InsightsDataIssueType.IncompleteUltrasoundData, incompleteUltrasoundCount(filter)),
        )

    fun recordRefs(
        filter: InsightsFilter,
        issueType: InsightsDataIssueType,
    ): List<InsightsRecordRef> =
        when (issueType) {
            InsightsDataIssueType.UnknownReproductionCategory -> unknownReproductionRefs(filter)
            InsightsDataIssueType.MissingVetName -> missingVetRefs(filter)
            InsightsDataIssueType.UnlinkedOwner -> unlinkedOwnerRefs(filter)
            InsightsDataIssueType.FreeTextEmbryoRecipient -> freeTextEmbryoRefs(filter)
            InsightsDataIssueType.IncompleteUltrasoundData -> incompleteUltrasoundRefs(filter)
        }

    private fun unknownReproductionCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectUnknownReproductionCountForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .toInt()
            },
            {
                database.insightsQueries
                    .selectUnknownReproductionCountGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .toInt()
            },
        )

    private fun missingVetCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectMissingVetCountForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .toInt()
            },
            {
                database.insightsQueries
                    .selectMissingVetCountGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .toInt()
            },
        )

    private fun unlinkedOwnerCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectUnlinkedOwnerCountForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .toInt()
            },
            {
                database.insightsQueries
                    .selectUnlinkedOwnerCountGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .toInt()
            },
        )

    private fun freeTextEmbryoCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectFreeTextEmbryoCountForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .toInt()
            },
            {
                database.insightsQueries
                    .selectFreeTextEmbryoCountGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .toInt()
            },
        )

    private fun incompleteUltrasoundCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectIncompleteUltrasoundCountForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .toInt()
            },
            {
                database.insightsQueries
                    .selectIncompleteUltrasoundCountGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .toInt()
            },
        )

    private fun unknownReproductionRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectUnknownReproductionRefsForPatient(filter.from, filter.to, patientId)
                    .executeAsList()
                    .map { row ->
                        InsightsRecordRef(RecordType.ReproductionEvent, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
            {
                database.insightsQueries
                    .selectUnknownReproductionRefsGlobal(filter.from, filter.to)
                    .executeAsList()
                    .map { row ->
                        InsightsRecordRef(RecordType.ReproductionEvent, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
        )

    private fun missingVetRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        activityRefs(
            filter,
            { patientId ->
                database.insightsQueries
                    .selectMissingVetRefsForPatient(filter.from, filter.to, patientId)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
            {
                database.insightsQueries
                    .selectMissingVetRefsGlobal(filter.from, filter.to)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
        )

    private fun unlinkedOwnerRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        activityRefs(
            filter,
            { patientId ->
                database.insightsQueries
                    .selectUnlinkedOwnerRefsForPatient(filter.from, filter.to, patientId)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
            {
                database.insightsQueries
                    .selectUnlinkedOwnerRefsGlobal(filter.from, filter.to)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
        )

    private fun freeTextEmbryoRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        activityRefs(
            filter,
            { patientId ->
                database.insightsQueries
                    .selectFreeTextEmbryoRefsForPatient(filter.from, filter.to, patientId)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
            {
                database.insightsQueries
                    .selectFreeTextEmbryoRefsGlobal(filter.from, filter.to)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
        )

    private fun incompleteUltrasoundRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        activityRefs(
            filter,
            { patientId ->
                database.insightsQueries
                    .selectIncompleteUltrasoundRefsForPatient(filter.from, filter.to, patientId)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
            {
                database.insightsQueries
                    .selectIncompleteUltrasoundRefsGlobal(filter.from, filter.to)
                    .executeAsList()
                    .mapNotNull { row ->
                        mapRecordRef(row.recordTypeWireName, row.patientId, row.recordId, row.patientName, row.date)
                    }
            },
        )

    private fun activityRefs(
        filter: InsightsFilter,
        scoped: (Long) -> List<InsightsRecordRef>,
        global: () -> List<InsightsRecordRef>,
    ): List<InsightsRecordRef> = withScope(filter.patientId, scoped, global)

    private inline fun <T> withScope(
        patientId: Long?,
        scoped: (Long) -> T,
        global: () -> T,
    ): T = if (patientId == null) global() else scoped(patientId)
}

private fun countOrNull(
    type: InsightsDataIssueType,
    count: Int,
): InsightsDataIssueCount? = if (count > 0) InsightsDataIssueCount(type, count) else null

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
