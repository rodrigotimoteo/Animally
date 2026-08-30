package com.github.rodrigotimoteo.animally.domain.timeline.usecase

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineFeed
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineGroup
import com.github.rodrigotimoteo.animally.domain.timeline.provider.TimelineProviderRegistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Facade that aggregates every record type into a date-grouped timeline.
 *
 * Data fetching and mapping lives in [TimelineProviderRegistry]; this class
 * only resolves patient names, loops the registry, and builds the feed.
 *
 * Records without a natural date (anamnese) are excluded. Medication entries
 * are only included when a start date is set.
 *
 * @param database The database used to read all record types and patient names.
 */
@Single
class GetTimelineUseCase(
    @Provided private val database: AnimallyDatabase,
) {
    /**
     * Builds the timeline feed for a single patient.
     *
     * @param patientId The identifier of the patient.
     * @return The [TimelineFeed] for the patient, with groups sorted by date descending.
     */
    operator fun invoke(patientId: Long): TimelineFeed {
        val patientName =
            database.patientQueries
                .selectById(patientId)
                .executeAsOneOrNull()
                ?.name
        val entries =
            collectEntries(
                patientId = patientId,
                patientName = patientName,
                patientNames = emptyMap(),
            )
        return buildFeed(patientId, patientName, entries)
    }

    /**
     * Builds the global timeline feed across all patients.
     *
     * @return The [TimelineFeed] with `patientId` and `patientName` set to `null`; every entry
     * carries its owning patient name.
     */
    operator fun invoke(): TimelineFeed {
        val patientNames =
            database.patientQueries
                .selectAll()
                .executeAsList()
                .associate { it.id to it.name }
        val entries =
            collectEntries(
                patientId = null,
                patientName = null,
                patientNames = patientNames,
            )
        return buildFeed(patientId = null, patientName = null, entries = entries)
    }

    /**
     * Collects entries via the provider registry. The patient-scoped and global
     * feeds share the same loop; only the name resolver differs.
     */
    private fun collectEntries(
        patientId: Long?,
        patientName: String?,
        patientNames: Map<Long, String>,
    ): List<TimelineEntry> {
        val nameFor: (Long) -> String =
            if (patientId == null) {
                { id -> patientNames[id].orEmpty() }
            } else {
                { _ -> patientName.orEmpty() }
            }
        val sink = mutableListOf<TimelineEntry>()
        TimelineProviderRegistry.providers.forEach { provider ->
            provider.collect(patientId, nameFor, database, sink)
        }
        return sink
    }

    private fun buildFeed(
        patientId: Long?,
        patientName: String?,
        entries: List<TimelineEntry>,
    ): TimelineFeed {
        val groups =
            entries
                .groupBy { entry -> entry.date }
                .map { (date, dateEntries) -> TimelineGroup(date, dateEntries) }
                .sortedByDescending { group -> group.date }
        return TimelineFeed(patientId, patientName, groups)
    }
}
