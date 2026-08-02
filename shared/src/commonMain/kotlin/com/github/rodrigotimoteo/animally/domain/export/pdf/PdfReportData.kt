package com.github.rodrigotimoteo.animally.domain.export.pdf

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.datetime.LocalDate

/**
 * Platform-agnostic payload for a patient history PDF document.
 *
 * Carries the patient demographics plus one [PdfSection] per record entity,
 * so the platform PDF renderers never touch domain repositories.
 *
 * @property patient the patient's demographics, printed as the document header.
 * @property sections the per-entity record tables; the first row of each
 * section is its header row.
 * @property fromDate inclusive lower date bound of the exported range, if any.
 * @property toDate inclusive upper date bound of the exported range, if any.
 */
data class PdfReportData(
    val patient: PdfPatient,
    val sections: List<PdfSection>,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
)

/**
 * A titled table inside the PDF report.
 *
 * @property title the section title, drawn as a heading.
 * @property rows the table rows as `List<String>` cells; the first row is the
 * header row and the remaining rows hold record data.
 */
data class PdfSection(
    val title: String,
    val rows: List<List<String>>,
)

/**
 * Demographics block of the exported patient, decoupled from the domain
 * [Patient] so the PDF DTO can be shared and tested without repositories.
 */
data class PdfPatient(
    val name: String,
    val species: String,
    val breed: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val microchipId: String? = null,
    val ueln: String? = null,
    val registrationNumber: String? = null,
    val stableLocation: String? = null,
    val notes: String? = null,
) {
    companion object {
        fun from(patient: Patient): PdfPatient =
            PdfPatient(
                name = patient.name,
                species = patient.species,
                breed = patient.breed,
                dateOfBirth = patient.dateOfBirth,
                gender = patient.gender,
                microchipId = patient.microchipId,
                ueln = patient.ueln,
                registrationNumber = patient.registrationNumber,
                stableLocation = patient.stableLocation,
                notes = patient.notes,
            )
    }
}
