package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate

/**
 * Immutable snapshot of every per-patient record list to include in a CSV export.
 *
 * The patient itself is not part of this snapshot; it is passed separately to
 * [CsvExporter.exportPatientRecords].
 *
 * @property anamnese The patient's anamnese record, if any (1:1 with the patient).
 * @property weights Historical weight measurements.
 * @property consultations SOAP-documented consultations.
 * @property vaccinations Vaccination records.
 * @property dewormings Deworming (anthelmintic) records.
 * @property dentistries Dental check and treatment records.
 * @property lamenesses Lameness evaluations.
 * @property surgeries Surgery records.
 * @property medications Prescribed medications.
 * @property labResults Laboratory test results.
 * @property imagings Diagnostic imaging records.
 * @property farrierVisits Farrier (hoof care) visits.
 * @property reproductionEvents Breeding-cycle events.
 * @property ultrasounds Reproductive ultrasound examinations.
 * @property gestations Pregnancy (gestation) records.
 * @property reproMedications Reproduction-related medication administrations.
 * @property controlledSubstances Regulated controlled substance administrations.
 */
data class ExportRecords(
    val anamnese: List<Anamnese> = emptyList(),
    val weights: List<Weight> = emptyList(),
    val consultations: List<Consultation> = emptyList(),
    val vaccinations: List<Vaccination> = emptyList(),
    val dewormings: List<Deworming> = emptyList(),
    val dentistries: List<Dentistry> = emptyList(),
    val lamenesses: List<Lameness> = emptyList(),
    val surgeries: List<Surgery> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val labResults: List<LabResult> = emptyList(),
    val imagings: List<Imaging> = emptyList(),
    val farrierVisits: List<FarrierVisit> = emptyList(),
    val reproductionEvents: List<ReproductionEvent> = emptyList(),
    val ultrasounds: List<Ultrasound> = emptyList(),
    val gestations: List<Gestation> = emptyList(),
    val reproMedications: List<ReproMedication> = emptyList(),
    val controlledSubstances: List<ControlledSubstance> = emptyList(),
)

/**
 * Returns a copy of this snapshot with every dated record list filtered to the
 * inclusive [from]/[to] range. Records without a meaningful date field
 * (anamnese) are always kept.
 *
 * Shared by the CSV and PDF exporters so both formats apply identical rules.
 */
internal fun ExportRecords.filterByDate(
    from: LocalDate?,
    to: LocalDate?,
): ExportRecords =
    ExportRecords(
        anamnese = anamnese,
        weights = weights.filter { inRange(it.date, from, to) },
        consultations = consultations.filter { inRange(it.date, from, to) },
        vaccinations = vaccinations.filter { inRange(it.dateAdministered, from, to) },
        dewormings = dewormings.filter { inRange(it.dateAdministered, from, to) },
        dentistries = dentistries.filter { inRange(it.date, from, to) },
        lamenesses = lamenesses.filter { inRange(it.date, from, to) },
        surgeries = surgeries.filter { inRange(it.date, from, to) },
        medications = medications.filter { it.startDate == null || inRange(it.startDate, from, to) },
        labResults = labResults.filter { inRange(it.date, from, to) },
        imagings = imagings.filter { inRange(it.date, from, to) },
        farrierVisits = farrierVisits.filter { inRange(it.date, from, to) },
        reproductionEvents = reproductionEvents.filter { inRange(it.date, from, to) },
        ultrasounds = ultrasounds.filter { inRange(it.date, from, to) },
        gestations = gestations.filter { inRange(it.breedingDate, from, to) },
        reproMedications = reproMedications.filter { inRange(it.dateAdministered, from, to) },
        controlledSubstances = controlledSubstances.filter { inRange(it.date, from, to) },
    )

private fun inRange(
    date: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
): Boolean = (from == null || date >= from) && (to == null || date <= to)
