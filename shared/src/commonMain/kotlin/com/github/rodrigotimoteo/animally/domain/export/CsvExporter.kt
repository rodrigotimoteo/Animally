package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Single

/**
 * Renders patient records as a single CSV document.
 *
 * The document is split into one section per entity type. Each section starts
 * with a `# SectionTitle` comment line, followed by a header row and one row
 * per record. Sections are emitted even when a record list is empty (header
 * row only), so the export has a stable shape.
 *
 * All formatting and escaping is delegated to [CsvFormatter].
 */
@Single
class CsvExporter {
    /**
     * Exports one [patient] and its [records] as CSV.
     *
     * @param patient the patient to export (emitted in the "Patient" section).
     * @param records the per-patient record lists to export.
     * @return the full CSV document.
     */
    fun exportPatientRecords(
        patient: Patient,
        records: ExportRecords,
    ): String {
        val lines = mutableListOf<String>()
        appendSection(lines, "Patient", PATIENT_HEADERS, listOf(patientRow(patient)))
        appendBasicSections(lines, records)
        appendClinicalSections(lines, records)
        appendReproductiveSections(lines, records)
        return lines.joinToString(separator = "")
    }

    private fun appendBasicSections(
        lines: MutableList<String>,
        records: ExportRecords,
    ) {
        appendSection(lines, "Anamnese", ANAMNESE_HEADERS, records.anamnese.map(::anamneseRow))
        appendSection(lines, "Weight", WEIGHT_HEADERS, records.weights.map(::weightRow))
        appendSection(lines, "Consultation", CONSULTATION_HEADERS, records.consultations.map(::consultationRow))
        appendSection(lines, "Vaccination", VACCINATION_HEADERS, records.vaccinations.map(::vaccinationRow))
        appendSection(lines, "Deworming", DEWORMING_HEADERS, records.dewormings.map(::dewormingRow))
        appendSection(lines, "Dentistry", DENTISTRY_HEADERS, records.dentistries.map(::dentistryRow))
    }

    private fun appendClinicalSections(
        lines: MutableList<String>,
        records: ExportRecords,
    ) {
        appendSection(lines, "Lameness", LAMENESS_HEADERS, records.lamenesses.map(::lamenessRow))
        appendSection(lines, "Surgery", SURGERY_HEADERS, records.surgeries.map(::surgeryRow))
        appendSection(lines, "Medication", MEDICATION_HEADERS, records.medications.map(::medicationRow))
        appendSection(lines, "LabResult", LABRESULT_HEADERS, records.labResults.map(::labResultRow))
        appendSection(lines, "Imaging", IMAGING_HEADERS, records.imagings.map(::imagingRow))
        appendSection(lines, "FarrierVisit", FARRIER_HEADERS, records.farrierVisits.map(::farrierRow))
    }

    private fun appendReproductiveSections(
        lines: MutableList<String>,
        records: ExportRecords,
    ) {
        appendSection(
            lines,
            "ReproductionEvent",
            REPRODUCTION_HEADERS,
            records.reproductionEvents.map(::reproductionEventRow),
        )
        appendSection(lines, "Ultrasound", ULTRASOUND_HEADERS, records.ultrasounds.map(::ultrasoundRow))
        appendSection(lines, "Gestation", GESTATION_HEADERS, records.gestations.map(::gestationRow))
        appendSection(
            lines,
            "ReproMedication",
            REPROMEDICATION_HEADERS,
            records.reproMedications.map(::reproMedicationRow),
        )
        appendSection(
            lines,
            "ControlledSubstance",
            CONTROLLED_SUBSTANCE_HEADERS,
            records.controlledSubstances.map(::controlledSubstanceRow),
        )
    }

    /**
     * Appends one section: a `# [title]` comment line, a display header row
     * led by [CsvFormatter.RECORD_TYPE_HEADER], then one row per record with
     * the record type as its first cell.
     */
    private fun appendSection(
        lines: MutableList<String>,
        title: String,
        headers: List<String>,
        rows: List<List<Any?>>,
    ) {
        lines += "# $title\r\n"
        lines += CsvFormatter.line(listOf(CsvFormatter.RECORD_TYPE_HEADER) + CsvFormatter.displayHeaders(headers))
        rows.forEach { lines += CsvFormatter.line(listOf(title) + it) }
    }
}
