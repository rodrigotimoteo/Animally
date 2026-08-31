package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionEventCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.insights.model.avgOrNull
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType

internal class SqlDelightInsightsReproductionReader(
    private val database: AnimallyDatabase,
) {
    fun read(filter: InsightsFilter): ReproductionMetrics {
        val embryos = embryoStats(filter)
        val icsi = icsiStats(filter)
        return ReproductionMetrics(
            eventCounts = eventCounts(filter),
            embryoCollections = embryos.groups,
            embryosCollected = embryos.total,
            averageEmbryosPerCollection = avgOrNull(embryos.total, embryos.groups),
            icsiSessions = icsi.groups,
            folliclesRecovered = icsi.total,
            averageFolliclesPerIcsi = avgOrNull(icsi.total, icsi.groups),
            ultrasoundCount = ultrasoundCount(filter),
        )
    }

    private fun eventCounts(filter: InsightsFilter): List<ReproductionEventCount> {
        val rows =
            withScope(
                filter.patientId,
                { patientId ->
                    database.insightsQueries
                        .selectReproductionEventCountsForPatient(filter.from, filter.to, patientId)
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
        return rows
            .groupBy { ReproductionEventType.from(it.first) }
            .map { (type, values) -> ReproductionEventCount(type, values.sumOf { it.second.toInt() }) }
            .sortedWith(compareByDescending<ReproductionEventCount> { it.count }.thenBy { it.type.storageLabel })
    }

    private fun embryoStats(filter: InsightsFilter): AggregateStats =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectEmbryoStatsForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .let { AggregateStats(it.collections.toInt(), it.totalEmbryos.toInt()) }
            },
            {
                database.insightsQueries
                    .selectEmbryoStatsGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .let { AggregateStats(it.collections.toInt(), it.totalEmbryos.toInt()) }
            },
        )

    private fun icsiStats(filter: InsightsFilter): AggregateStats =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectIcsiStatsForPatient(filter.from, filter.to, patientId)
                    .executeAsOne()
                    .let { AggregateStats(it.sessions.toInt(), it.totalFollicles.toInt()) }
            },
            {
                database.insightsQueries
                    .selectIcsiStatsGlobal(filter.from, filter.to)
                    .executeAsOne()
                    .let { AggregateStats(it.sessions.toInt(), it.totalFollicles.toInt()) }
            },
        )

    private fun ultrasoundCount(filter: InsightsFilter): Int =
        withScope(
            filter.patientId,
            { patientId ->
                database.insightsQueries
                    .selectUltrasoundCountForPatient(filter.from, filter.to, patientId)
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

    private inline fun <T> withScope(
        patientId: Long?,
        scoped: (Long) -> T,
        global: () -> T,
    ): T = if (patientId == null) global() else scoped(patientId)
}

private data class AggregateStats(
    val groups: Int,
    val total: Int,
)
