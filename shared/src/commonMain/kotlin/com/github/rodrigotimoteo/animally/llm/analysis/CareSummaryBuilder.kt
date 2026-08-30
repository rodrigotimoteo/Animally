package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.llm.RagDateRange
import com.github.rodrigotimoteo.animally.llm.support.DateFormatting

internal class CareSummaryBuilder(
    private val vaccinationRepository: IVaccinationRepository,
    private val dewormingRepository: IDewormingRepository,
    private val farrierVisitRepository: IFarrierVisitRepository,
) {
    private data class CarePatientSummary(
        val patient: Patient,
        val vaccinations: List<Vaccination>,
        val dewormings: List<Deworming>,
        val farrierVisits: List<FarrierVisit>,
    ) {
        val hasRecords: Boolean
            get() = vaccinations.isNotEmpty() || dewormings.isNotEmpty() || farrierVisits.isNotEmpty()
    }

    fun careBlock(
        targets: List<Patient>,
        dateRange: RagDateRange?,
    ): String? {
        if (targets.isEmpty()) return null
        val summaries = targets.map { patient -> careSummaryForPatient(patient, dateRange) }
        val withRecords = summaries.filter(CarePatientSummary::hasRecords)
        if (withRecords.isEmpty()) return "CARE COUNTS: no care records found for the selected patients."
        val lines = withRecords.take(MAX_PATIENTS_SCANNED).map(::careLineForPatient)
        val omitted = withRecords.size - lines.size
        val vaccinationCount = withRecords.sumOf { it.vaccinations.size }
        val dewormingCount = withRecords.sumOf { it.dewormings.size }
        val farrierCount = withRecords.sumOf { it.farrierVisits.size }
        return buildString {
            appendLine(
                "CARE TOTALS: $vaccinationCount vaccinations, $dewormingCount dewormings, " +
                    "$farrierCount farrier visits across ${withRecords.size} patients with records.",
            )
            appendLine("CARE COUNTS:")
            lines.forEach(::appendLine)
            if (omitted > 0) {
                appendLine("- CARE DETAILS TRUNCATED: $omitted more patients are included in the totals above.")
            }
        }.trimEnd()
    }

    private fun careSummaryForPatient(
        patient: Patient,
        dateRange: RagDateRange?,
    ): CarePatientSummary =
        CarePatientSummary(
            patient = patient,
            vaccinations =
                vaccinationRepository
                    .getByPatient(patient.id)
                    .filter { vaccination -> dateRange?.contains(vaccination.dateAdministered) != false },
            dewormings =
                dewormingRepository
                    .getByPatient(patient.id)
                    .filter { deworming -> dateRange?.contains(deworming.dateAdministered) != false },
            farrierVisits =
                farrierVisitRepository
                    .getByPatient(patient.id)
                    .filter { farrierVisit -> dateRange?.contains(farrierVisit.date) != false },
        )

    private fun careLineForPatient(summary: CarePatientSummary): String {
        val parts =
            listOf(
                carePart(
                    summary.vaccinations.size,
                    "vaccinations",
                    summary.vaccinations.maxOfOrNull { it.dateAdministered },
                ),
                carePart(
                    summary.dewormings.size,
                    "dewormings",
                    summary.dewormings.maxOfOrNull { it.dateAdministered },
                ),
                carePart(summary.farrierVisits.size, "farrier visits", summary.farrierVisits.maxOfOrNull { it.date }),
            )
        return "- Care ${summary.patient.name}: ${parts.joinToString(", ")}."
    }

    private fun carePart(
        count: Int,
        label: String,
        lastDate: kotlinx.datetime.LocalDate?,
    ): String = "$count $label" + (lastDate?.let { " (last ${DateFormatting.formatHumanDate(it)})" } ?: "")
}
