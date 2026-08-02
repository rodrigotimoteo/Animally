package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.export.pdf.PdfSection

/**
 * Maps a record snapshot to the section tables of a PDF report.
 *
 * Sections are emitted only when the entity has at least one record, so empty
 * tables stay out of the rendered document. Column headers are the same as the
 * CSV export ([PATIENT_HEADERS] et al.) to keep both formats consistent.
 */
internal fun buildPdfSections(records: ExportRecords): List<PdfSection> =
    buildBasicSections(records) + buildClinicalSections(records) + buildReproSections(records)

private fun buildBasicSections(records: ExportRecords): List<PdfSection> =
    buildList {
        if (records.anamnese.isNotEmpty()) {
            add(PdfSection("Anamnese", renderRows(ANAMNESE_HEADERS, records.anamnese.map(::anamneseRow))))
        }
        if (records.weights.isNotEmpty()) {
            add(PdfSection("Weight", renderRows(WEIGHT_HEADERS, records.weights.map(::weightRow))))
        }
        if (records.consultations.isNotEmpty()) {
            add(
                PdfSection(
                    "Consultation",
                    renderRows(CONSULTATION_HEADERS, records.consultations.map(::consultationRow)),
                ),
            )
        }
        if (records.vaccinations.isNotEmpty()) {
            add(PdfSection("Vaccination", renderRows(VACCINATION_HEADERS, records.vaccinations.map(::vaccinationRow))))
        }
        if (records.dewormings.isNotEmpty()) {
            add(PdfSection("Deworming", renderRows(DEWORMING_HEADERS, records.dewormings.map(::dewormingRow))))
        }
        if (records.dentistries.isNotEmpty()) {
            add(PdfSection("Dentistry", renderRows(DENTISTRY_HEADERS, records.dentistries.map(::dentistryRow))))
        }
    }

private fun buildClinicalSections(records: ExportRecords): List<PdfSection> =
    buildList {
        if (records.lamenesses.isNotEmpty()) {
            add(PdfSection("Lameness", renderRows(LAMENESS_HEADERS, records.lamenesses.map(::lamenessRow))))
        }
        if (records.surgeries.isNotEmpty()) {
            add(PdfSection("Surgery", renderRows(SURGERY_HEADERS, records.surgeries.map(::surgeryRow))))
        }
        if (records.medications.isNotEmpty()) {
            add(PdfSection("Medication", renderRows(MEDICATION_HEADERS, records.medications.map(::medicationRow))))
        }
        if (records.labResults.isNotEmpty()) {
            add(PdfSection("Lab Result", renderRows(LABRESULT_HEADERS, records.labResults.map(::labResultRow))))
        }
        if (records.imagings.isNotEmpty()) {
            add(PdfSection("Imaging", renderRows(IMAGING_HEADERS, records.imagings.map(::imagingRow))))
        }
        if (records.farrierVisits.isNotEmpty()) {
            add(PdfSection("Farrier Visit", renderRows(FARRIER_HEADERS, records.farrierVisits.map(::farrierRow))))
        }
    }

private fun buildReproSections(records: ExportRecords): List<PdfSection> =
    buildList {
        if (records.reproductionEvents.isNotEmpty()) {
            add(
                PdfSection(
                    "Reproduction Event",
                    renderRows(REPRODUCTION_HEADERS, records.reproductionEvents.map(::reproductionEventRow)),
                ),
            )
        }
        if (records.ultrasounds.isNotEmpty()) {
            add(PdfSection("Ultrasound", renderRows(ULTRASOUND_HEADERS, records.ultrasounds.map(::ultrasoundRow))))
        }
        if (records.gestations.isNotEmpty()) {
            add(PdfSection("Gestation", renderRows(GESTATION_HEADERS, records.gestations.map(::gestationRow))))
        }
        if (records.reproMedications.isNotEmpty()) {
            add(
                PdfSection(
                    "Repro Medication",
                    renderRows(REPROMEDICATION_HEADERS, records.reproMedications.map(::reproMedicationRow)),
                ),
            )
        }
        if (records.controlledSubstances.isNotEmpty()) {
            add(
                PdfSection(
                    "Controlled Substance",
                    renderRows(
                        CONTROLLED_SUBSTANCE_HEADERS,
                        records.controlledSubstances.map(::controlledSubstanceRow),
                    ),
                ),
            )
        }
    }

/**
 * Renders one table: the [headers] row followed by each [rows] entry with
 * `null` cells collapsed to an empty string.
 */
internal fun renderRows(
    headers: List<String>,
    rows: List<List<Any?>>,
): List<List<String>> = listOf(headers) + rows.map { row -> row.map { it?.toString().orEmpty() } }
