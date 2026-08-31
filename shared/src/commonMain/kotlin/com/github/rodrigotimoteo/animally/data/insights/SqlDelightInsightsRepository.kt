package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.insights.mapper.buildReproductionMetrics
import com.github.rodrigotimoteo.animally.data.insights.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.insights.mapper.toDomainFiltered
import com.github.rodrigotimoteo.animally.data.insights.mapper.toReproductionEventCountsForPatient
import com.github.rodrigotimoteo.animally.data.insights.mapper.toReproductionEventCountsGlobal
import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount
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

/**
 * SQLDelight implementation of [IInsightsRepository].
 *
 * Aggregation is performed in SQL via `WITH activity_rows AS (UNION ALL ...)` over
 * 17 active dated tables. Each branch filters `isActive = 1`, inclusive date range,
 * sargable patient scope (scoped vs global variants), and
 * `Medication.startDate IS NOT NULL`. Patient activity is required (`Patient.isActive = 1`)
 * via a single join outside the union. Gestation/anamnese/customReminder/follicle excluded.
 * M6: patient scope split into SEARCH-friendly equality vs no predicate (no OR IS NULL).
 *
 * Reproduction period facts (Task 10) follow same sargable split: scoped/global variants
 * for event counts, embryo/ICSI aggregates and ultrasound counts, canonicalising
 * [com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType] tolerant to
 * legacy spellings (unknown -> Other) and excluding soft-deleted/out-of-range/inactive-patient rows.
 * No success or outcome rate is derived.
 *
 * Stable [com.github.rodrigotimoteo.animally.domain.common.RecordType.wireName] literals are emitted
 * in SQL and parsed with `RecordType.fromWireName`; unknown wire names are filtered out.
 */
@Suppress("TooManyFunctions")
@Single(binds = [IInsightsRepository::class])
class SqlDelightInsightsRepository(
    @Provided private val database: AnimallyDatabase,
) : IInsightsRepository {
    override fun getEarliestActivityDate(patientId: Long?): LocalDate? =
        if (patientId == null) {
            database.insightsQueries.selectEarliestActivityDateGlobal().executeAsOneOrNull()
        } else {
            database.insightsQueries.selectEarliestActivityDateForPatient(patientId).executeAsOneOrNull()
        }

    override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectActivityBucketsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomain() }
        } else {
            database.insightsQueries
                .selectActivityBucketsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain() }
        }

    override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> =
        if (drillDown.dataIssueType != null) {
            getDataIssueRecordRefs(
                InsightsFilter(from = drillDown.from, to = drillDown.to, patientId = drillDown.patientId),
                drillDown.dataIssueType,
            )
        } else if (drillDown.reproductionEventType != null) {
            getReproductionEventRefs(drillDown)
        } else if (drillDown.patientId == null) {
            val wire = drillDown.recordType?.wireName
            if (wire == null) {
                database.insightsQueries
                    .selectActivityRecordRefsGlobal(
                        from = drillDown.from,
                        to = drillDown.to,
                    ).executeAsList()
                    .mapNotNull { it.toDomain() }
            } else {
                database.insightsQueries
                    .selectActivityRecordRefsGlobalByType(
                        from = drillDown.from,
                        to = drillDown.to,
                        recordTypeWireName = wire,
                    ).executeAsList()
                    .mapNotNull { it.toDomain() }
            }
        } else {
            val wire = drillDown.recordType?.wireName
            if (wire == null) {
                database.insightsQueries
                    .selectActivityRecordRefsForPatient(
                        from = drillDown.from,
                        to = drillDown.to,
                        patientId = drillDown.patientId,
                    ).executeAsList()
                    .mapNotNull { it.toDomain() }
            } else {
                database.insightsQueries
                    .selectActivityRecordRefsForPatientByType(
                        from = drillDown.from,
                        to = drillDown.to,
                        patientId = drillDown.patientId,
                        recordTypeWireName = wire,
                    ).executeAsList()
                    .mapNotNull { it.toDomain() }
            }
        }

    private fun getReproductionEventRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> {
        val requiredType = requireNotNull(drillDown.reproductionEventType)
        return if (drillDown.patientId == null) {
            database.insightsQueries
                .selectReproductionRecordRefsGlobal(from = drillDown.from, to = drillDown.to)
                .executeAsList()
                .mapNotNull { it.toDomain(requiredType) }
        } else {
            database.insightsQueries
                .selectReproductionRecordRefsForPatient(
                    from = drillDown.from,
                    to = drillDown.to,
                    patientId = drillDown.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain(requiredType) }
        }
    }

    @Suppress("LongMethod")
    override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics {
        val eventCounts =
            if (filter.patientId == null) {
                database.insightsQueries
                    .selectReproductionEventCountsGlobal(from = filter.from, to = filter.to)
                    .executeAsList()
                    .toReproductionEventCountsGlobal()
            } else {
                database.insightsQueries
                    .selectReproductionEventCountsForPatient(
                        from = filter.from,
                        to = filter.to,
                        patientId = filter.patientId,
                    ).executeAsList()
                    .toReproductionEventCountsForPatient()
            }
        val (embryoCollections, embryosCollected) =
            if (filter.patientId == null) {
                database.insightsQueries
                    .selectEmbryoStatsGlobal(from = filter.from, to = filter.to)
                    .executeAsOne()
                    .toDomain()
            } else {
                database.insightsQueries
                    .selectEmbryoStatsForPatient(
                        from = filter.from,
                        to = filter.to,
                        patientId = filter.patientId,
                    ).executeAsOne()
                    .toDomain()
            }
        val (icsiSessions, folliclesRecovered) =
            if (filter.patientId == null) {
                database.insightsQueries
                    .selectIcsiStatsGlobal(from = filter.from, to = filter.to)
                    .executeAsOne()
                    .toDomain()
            } else {
                database.insightsQueries
                    .selectIcsiStatsForPatient(
                        from = filter.from,
                        to = filter.to,
                        patientId = filter.patientId,
                    ).executeAsOne()
                    .toDomain()
            }
        val ultrasoundCount =
            if (filter.patientId == null) {
                database.insightsQueries
                    .selectUltrasoundCountGlobal(from = filter.from, to = filter.to)
                    .executeAsOne()
                    .toInt()
            } else {
                database.insightsQueries
                    .selectUltrasoundCountForPatient(
                        from = filter.from,
                        to = filter.to,
                        patientId = filter.patientId,
                    ).executeAsOne()
                    .toInt()
            }
        return buildReproductionMetrics(
            eventCounts = eventCounts,
            embryoCollections = embryoCollections,
            embryosCollected = embryosCollected,
            icsiSessions = icsiSessions,
            folliclesRecovered = folliclesRecovered,
            ultrasoundCount = ultrasoundCount,
        )
    }

    @Suppress("MagicNumber")
    override fun getCurrentCareSnapshot(
        patientId: Long?,
        today: LocalDate,
    ): CurrentCareSnapshot {
        // Status filter pushed to SQL (LOWER + NOT IN) case-insensitive; covering indexes:
        // idx_gestation_patient_expected_due_date WHERE isActive=1, idx_gestation_expected_due_date.
        val items: List<CurrentGestationItem> =
            if (patientId == null) {
                database.gestationQueries
                    .selectActiveGestationsGlobal()
                    .executeAsList()
                    .map { row ->
                        val breeding = row.breedingDate
                        val gestationDay = breeding.daysUntil(today).coerceAtLeast(0)
                        val dueDate = row.expectedDueDate
                        val daysUntilDue = today.daysUntil(dueDate)
                        CurrentGestationItem(
                            patientId = row.patientId,
                            patientName = row.patientName,
                            gestationId = row.gestationId,
                            gestationDay = gestationDay,
                            dueDate = dueDate,
                            daysUntilDue = daysUntilDue,
                            status = row.status ?: "",
                        )
                    }
            } else {
                database.gestationQueries
                    .selectActiveGestationsForPatient(patientId)
                    .executeAsList()
                    .map { row ->
                        val breeding = row.breedingDate
                        val gestationDay = breeding.daysUntil(today).coerceAtLeast(0)
                        val dueDate = row.expectedDueDate
                        val daysUntilDue = today.daysUntil(dueDate)
                        CurrentGestationItem(
                            patientId = row.patientId,
                            patientName = row.patientName,
                            gestationId = row.gestationId,
                            gestationDay = gestationDay,
                            dueDate = dueDate,
                            daysUntilDue = daysUntilDue,
                            status = row.status ?: "",
                        )
                    }
            }.sortedBy { it.dueDate }
        // Due-soon is 0..N inclusive, sorted; overdue (negative) excluded by range start 0.
        // Kept in Kotlin (not pushed to SQL WHERE daysUntilDue BETWEEN 0 AND :days) — gestation
        // active rows are tiny (typically <~100), so SQL push saves negligible work and adds
        // 3 extra queries or complex UNION; in-memory filter is simpler and keeps ordering stable.
        return CurrentCareSnapshot(
            activeGestations = items,
            dueSoon30 = dueSoon(items, 30),
            dueSoon60 = dueSoon(items, 60),
            dueSoon90 = dueSoon(items, 90),
        )
    }

    override fun getDataIssueCounts(filter: InsightsFilter): List<InsightsDataIssueCount> {
        val unknown = getUnknownReproductionCount(filter)
        val missingVet = getMissingVetCount(filter)
        val unlinked = getUnlinkedOwnerCount(filter)
        val freeText = getFreeTextEmbryoCount(filter)
        val incomplete = getIncompleteUltrasoundCount(filter)
        return listOf(
            InsightsDataIssueCount(InsightsDataIssueType.UnknownReproductionCategory, unknown),
            InsightsDataIssueCount(InsightsDataIssueType.MissingVetName, missingVet),
            InsightsDataIssueCount(InsightsDataIssueType.UnlinkedOwner, unlinked),
            InsightsDataIssueCount(InsightsDataIssueType.FreeTextEmbryoRecipient, freeText),
            InsightsDataIssueCount(InsightsDataIssueType.IncompleteUltrasoundData, incomplete),
        ).filter { it.count > 0 }
    }

    override fun getDataIssueRecordRefs(
        filter: InsightsFilter,
        issueType: InsightsDataIssueType,
    ): List<InsightsRecordRef> =
        when (issueType) {
            InsightsDataIssueType.UnknownReproductionCategory -> getUnknownReproductionRefs(filter)
            InsightsDataIssueType.MissingVetName -> getMissingVetRefs(filter)
            InsightsDataIssueType.UnlinkedOwner -> getUnlinkedOwnerRefs(filter)
            InsightsDataIssueType.FreeTextEmbryoRecipient -> getFreeTextEmbryoRefs(filter)
            InsightsDataIssueType.IncompleteUltrasoundData -> getIncompleteUltrasoundRefs(filter)
        }

    private fun getUnknownReproductionCount(filter: InsightsFilter): Int {
        val types =
            if (filter.patientId == null) {
                database.insightsQueries.selectReproductionEventTypesGlobal(from = filter.from, to = filter.to).executeAsList()
            } else {
                database.insightsQueries
                    .selectReproductionEventTypesForPatient(
                        from = filter.from,
                        to = filter.to,
                        patientId = filter.patientId,
                    ).executeAsList()
            }
        return types.count { ReproductionEventType.from(it) == ReproductionEventType.Other }
    }

    private fun getMissingVetCount(filter: InsightsFilter): Int =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectMissingVetCountGlobal(from = filter.from, to = filter.to)
                .executeAsOne()
                .toInt()
        } else {
            database.insightsQueries
                .selectMissingVetCountForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsOne()
                .toInt()
        }

    private fun getUnlinkedOwnerCount(filter: InsightsFilter): Int =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectUnlinkedOwnerCountGlobal(from = filter.from, to = filter.to)
                .executeAsOne()
                .toInt()
        } else {
            database.insightsQueries
                .selectUnlinkedOwnerCountForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsOne()
                .toInt()
        }

    private fun getFreeTextEmbryoCount(filter: InsightsFilter): Int =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectFreeTextEmbryoCountGlobal(from = filter.from, to = filter.to)
                .executeAsOne()
                .toInt()
        } else {
            database.insightsQueries
                .selectFreeTextEmbryoCountForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsOne()
                .toInt()
        }

    private fun getIncompleteUltrasoundCount(filter: InsightsFilter): Int =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectIncompleteUltrasoundCountGlobal(from = filter.from, to = filter.to)
                .executeAsOne()
                .toInt()
        } else {
            database.insightsQueries
                .selectIncompleteUltrasoundCountForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsOne()
                .toInt()
        }

    private fun getUnknownReproductionRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectUnknownReproductionRefsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomainFiltered() }
        } else {
            database.insightsQueries
                .selectUnknownReproductionRefsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomainFiltered() }
        }

    private fun getMissingVetRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectMissingVetRefsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomain() }
        } else {
            database.insightsQueries
                .selectMissingVetRefsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain() }
        }

    private fun getUnlinkedOwnerRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectUnlinkedOwnerRefsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomain() }
        } else {
            database.insightsQueries
                .selectUnlinkedOwnerRefsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain() }
        }

    private fun getFreeTextEmbryoRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectFreeTextEmbryoRefsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomain() }
        } else {
            database.insightsQueries
                .selectFreeTextEmbryoRefsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain() }
        }

    private fun getIncompleteUltrasoundRefs(filter: InsightsFilter): List<InsightsRecordRef> =
        if (filter.patientId == null) {
            database.insightsQueries
                .selectIncompleteUltrasoundRefsGlobal(from = filter.from, to = filter.to)
                .executeAsList()
                .mapNotNull { it.toDomain() }
        } else {
            database.insightsQueries
                .selectIncompleteUltrasoundRefsForPatient(
                    from = filter.from,
                    to = filter.to,
                    patientId = filter.patientId,
                ).executeAsList()
                .mapNotNull { it.toDomain() }
        }

    private fun dueSoon(
        items: List<CurrentGestationItem>,
        days: Int,
    ): List<CurrentGestationItem> = items.filter { it.daysUntilDue in 0..days }
}
