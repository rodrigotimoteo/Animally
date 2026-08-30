package com.github.rodrigotimoteo.animally.domain.timeline.provider

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.customreminder.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.embryotransfer.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.icsi.mapper.toDomain
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

/**
 * Registry that enumerates every timeline provider in deterministic order.
 * [GetTimelineUseCase] iterates this list so adding a record type means
 * adding one provider here — no fan-out edits in the facade.
 */
internal object TimelineProviderRegistry {
    val providers: List<TimelineProvider> =
        listOf(
            ClinicalTimelineProvider(),
            FarrierTimelineProvider(),
            ReproductiveTimelineProvider(),
            PreventiveTimelineProvider(),
        )
}

/** Weight, deworming, dentistry, lameness, surgery, medication, lab result, imaging. */
@Suppress("LongMethod")
internal class ClinicalTimelineProvider : TimelineProvider {
    override fun collect(
        patientId: Long?,
        nameFor: (Long) -> String,
        database: AnimallyDatabase,
        sink: MutableList<TimelineEntry>,
    ) {
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.weightQueries.selectByPatient(pid).executeAsList() },
            all = { database.weightQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.dewormingQueries.selectByPatient(pid).executeAsList() },
            all = { database.dewormingQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.dentistryQueries.selectByPatient(pid).executeAsList() },
            all = { database.dentistryQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.lamenessQueries.selectByPatient(pid).executeAsList() },
            all = { database.lamenessQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.surgeryQueries.selectByPatient(pid).executeAsList() },
            all = { database.surgeryQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.medicationQueries.selectByPatient(pid).executeAsList() },
            all = { database.medicationQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntryOrNull(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.labResultQueries.selectByPatient(pid).executeAsList() },
            all = { database.labResultQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.imagingQueries.selectByPatient(pid).executeAsList() },
            all = { database.imagingQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
    }
}

/** Farrier visit entries. */
internal class FarrierTimelineProvider : TimelineProvider {
    override fun collect(
        patientId: Long?,
        nameFor: (Long) -> String,
        database: AnimallyDatabase,
        sink: MutableList<TimelineEntry>,
    ) {
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.farrierVisitQueries.selectByPatient(pid).executeAsList() },
            all = { database.farrierVisitQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
    }
}

/** Reproduction events, ultrasounds, gestations, repro medications, embryo transfers, ICSI. */
internal class ReproductiveTimelineProvider : TimelineProvider {
    override fun collect(
        patientId: Long?,
        nameFor: (Long) -> String,
        database: AnimallyDatabase,
        sink: MutableList<TimelineEntry>,
    ) {
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.reproductionQueries.selectByPatient(pid).executeAsList() },
            all = { database.reproductionQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.ultrasoundQueries.selectByPatient(pid).executeAsList() },
            all = { database.ultrasoundQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.gestationQueries.selectByPatient(pid).executeAsList() },
            all = { database.gestationQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.reproMedicationQueries.selectByPatient(pid).executeAsList() },
            all = { database.reproMedicationQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.embryoTransferQueries.selectByPatient(pid).executeAsList() },
            all = { database.embryoTransferQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.icsiQueries.selectByPatient(pid).executeAsList() },
            all = { database.icsiQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
    }
}

/** Controlled substances, consultations, vaccinations, custom reminders. */
internal class PreventiveTimelineProvider : TimelineProvider {
    override fun collect(
        patientId: Long?,
        nameFor: (Long) -> String,
        database: AnimallyDatabase,
        sink: MutableList<TimelineEntry>,
    ) {
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.substanceQueries.selectByPatient(pid).executeAsList() },
            all = { database.substanceQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.consultationQueries.selectByPatient(pid).executeAsList() },
            all = { database.consultationQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.vaccinationQueries.selectByPatient(pid).executeAsList() },
            all = { database.vaccinationQueries.selectAll().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
        collectEntries(
            patientId = patientId,
            nameFor = nameFor,
            sink = sink,
            byPatient = { pid -> database.customReminderQueries.selectByPatient(pid).executeAsList() },
            all = { database.customReminderQueries.selectAllActive().executeAsList() },
            patientIdOf = { it.patientId },
            toEntry = { row, name -> row.toDomain().toTimelineEntry(name) },
        )
    }
}

@Suppress("LongParameterList")
private fun <T> collectEntries(
    patientId: Long?,
    nameFor: (Long) -> String,
    sink: MutableList<TimelineEntry>,
    byPatient: (Long) -> List<T>,
    all: () -> List<T>,
    patientIdOf: (T) -> Long,
    toEntry: (T, String) -> TimelineEntry?,
) {
    val rows = if (patientId == null) all() else byPatient(patientId)
    rows.forEach { row -> toEntry(row, nameFor(patientIdOf(row)))?.let(sink::add) }
}
