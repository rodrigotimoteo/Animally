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
