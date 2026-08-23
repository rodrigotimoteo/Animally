package com.github.rodrigotimoteo.animally.domain.timeline.usecase

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.timeline.mapper.toTimelineEntry
import com.github.rodrigotimoteo.animally.domain.timeline.mapper.toTimelineEntryOrNull
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineFeed
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineGroup
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Aggregates every record type of a patient (or of all patients) into a date-grouped timeline.
 *
 * Records without a natural date (anamnese) are excluded from the feed. Medication entries are
 * only included when a start date is set.
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
     * Collects every entry for the patient feed ([patientId] set) or the global
     * feed ([patientId] null). Both variants share one collector: each record
     * type supplies its rows via fetch lambdas that pick `selectByPatient` or
     * `selectAll`, and a name resolver that is constant for the patient feed
     * and map-based for the global feed.
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
        val collector = EntryCollector(patientId, nameFor, database)
        with(collector) {
            clinical()
            farrier()
            reproductive()
            preventive()
        }
        return collector.entries
    }

    /** Shared per-feed collector: fetches rows and maps them to entries. */
    private class EntryCollector(
        private val patientId: Long?,
        private val nameFor: (Long) -> String,
        private val database: AnimallyDatabase,
    ) {
        val entries = mutableListOf<TimelineEntry>()

        fun <T> add(
            byPatient: (Long) -> List<T>,
            all: () -> List<T>,
            patientIdOf: (T) -> Long,
            toEntry: (T, String) -> TimelineEntry?,
        ) {
            val rows = if (patientId == null) all() else byPatient(patientId)
            rows.forEach { row -> toEntry(row, nameFor(patientIdOf(row)))?.let(entries::add) }
        }
    }

    /** Weight, deworming, dentistry, lameness, surgery, medication, lab result, imaging. */
    private fun EntryCollector.clinical() {
        add(
            { pid -> database.weightQueries.selectByPatient(pid).executeAsList() },
            { database.weightQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.dewormingQueries.selectByPatient(pid).executeAsList() },
            { database.dewormingQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.dentistryQueries.selectByPatient(pid).executeAsList() },
            { database.dentistryQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.lamenessQueries.selectByPatient(pid).executeAsList() },
            { database.lamenessQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.surgeryQueries.selectByPatient(pid).executeAsList() },
            { database.surgeryQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.medicationQueries.selectByPatient(pid).executeAsList() },
            { database.medicationQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntryOrNull(name) }
        add(
            { pid -> database.labResultQueries.selectByPatient(pid).executeAsList() },
            { database.labResultQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.imagingQueries.selectByPatient(pid).executeAsList() },
            { database.imagingQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
    }

    /** Farrier visit next-due entries. */
    private fun EntryCollector.farrier() {
        add(
            { pid -> database.farrierVisitQueries.selectByPatient(pid).executeAsList() },
            { database.farrierVisitQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
    }

    /** Reproduction events, ultrasounds, gestations, repro medications. */
    private fun EntryCollector.reproductive() {
        add(
            { pid -> database.reproductionQueries.selectByPatient(pid).executeAsList() },
            { database.reproductionQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.ultrasoundQueries.selectByPatient(pid).executeAsList() },
            { database.ultrasoundQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.gestationQueries.selectByPatient(pid).executeAsList() },
            { database.gestationQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.reproMedicationQueries.selectByPatient(pid).executeAsList() },
            { database.reproMedicationQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
    }

    /** Controlled substances, consultations, vaccinations. */
    private fun EntryCollector.preventive() {
        add(
            { pid -> database.substanceQueries.selectByPatient(pid).executeAsList() },
            { database.substanceQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.consultationQueries.selectByPatient(pid).executeAsList() },
            { database.consultationQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
        add(
            { pid -> database.vaccinationQueries.selectByPatient(pid).executeAsList() },
            { database.vaccinationQueries.selectAll().executeAsList() },
            { it.patientId },
        ) { row, name -> row.toDomain().toTimelineEntry(name) }
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
