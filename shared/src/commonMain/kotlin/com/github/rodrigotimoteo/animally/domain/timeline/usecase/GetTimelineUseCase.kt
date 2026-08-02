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
        val entries = collectPatient(patientId, patientName.orEmpty())
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
        return buildFeed(patientId = null, patientName = null, entries = collectGlobal(patientNames))
    }

    private fun collectPatient(
        patientId: Long,
        patientName: String,
    ): List<TimelineEntry> =
        buildList {
            addAll(clinicalPatientEntries(patientId, patientName))
            addAll(farrierPatientEntries(patientId, patientName))
            addAll(reproductivePatientEntries(patientId, patientName))
            addAll(preventivePatientEntries(patientId, patientName))
        }

    private fun collectGlobal(patientNames: Map<Long, String>): List<TimelineEntry> =
        buildList {
            addAll(clinicalGlobalEntries(patientNames))
            addAll(farrierGlobalEntries(patientNames))
            addAll(reproductiveGlobalEntries(patientNames))
            addAll(preventiveGlobalEntries(patientNames))
        }

    private fun clinicalPatientEntries(
        patientId: Long,
        patientName: String,
    ): List<TimelineEntry> =
        buildList {
            database.weightQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.dewormingQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.dentistryQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.lamenessQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.surgeryQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.medicationQueries.selectByPatient(patientId).executeAsList().forEach {
                it.toDomain().toTimelineEntryOrNull(patientName)?.let { entry -> add(entry) }
            }
            database.labResultQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.imagingQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
        }

    private fun farrierPatientEntries(
        patientId: Long,
        patientName: String,
    ): List<TimelineEntry> =
        buildList {
            database.farrierVisitQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
        }

    private fun reproductivePatientEntries(
        patientId: Long,
        patientName: String,
    ): List<TimelineEntry> =
        buildList {
            database.reproductionQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.ultrasoundQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.gestationQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.reproMedicationQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
        }

    private fun preventivePatientEntries(
        patientId: Long,
        patientName: String,
    ): List<TimelineEntry> =
        buildList {
            database.substanceQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.consultationQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
            database.vaccinationQueries.selectByPatient(patientId).executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientName))
            }
        }

    private fun clinicalGlobalEntries(patientNames: Map<Long, String>): List<TimelineEntry> =
        buildList {
            database.weightQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.dewormingQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.dentistryQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.lamenessQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.surgeryQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.medicationQueries.selectAll().executeAsList().forEach {
                it.toDomain().toTimelineEntryOrNull(patientNames[it.patientId].orEmpty())?.let { entry -> add(entry) }
            }
            database.labResultQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.imagingQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
        }

    private fun farrierGlobalEntries(patientNames: Map<Long, String>): List<TimelineEntry> =
        buildList {
            database.farrierVisitQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
        }

    private fun reproductiveGlobalEntries(patientNames: Map<Long, String>): List<TimelineEntry> =
        buildList {
            database.reproductionQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.ultrasoundQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.gestationQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.reproMedicationQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
        }

    private fun preventiveGlobalEntries(patientNames: Map<Long, String>): List<TimelineEntry> =
        buildList {
            database.substanceQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.consultationQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
            database.vaccinationQueries.selectAll().executeAsList().forEach {
                add(it.toDomain().toTimelineEntry(patientNames[it.patientId].orEmpty()))
            }
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
