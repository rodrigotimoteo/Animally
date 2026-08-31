package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.model.isActiveGestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.llm.support.DateFormatting
import kotlinx.datetime.LocalDate

internal class GestationSummaryBuilder(
    private val gestationRepository: IGestationRepository,
    private val calculateGestationUseCase: CalculateGestationUseCase,
) {
    private data class GestationSummaryRow(
        val patient: Patient,
        val gestation: Gestation,
    )

    fun gestationBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val rows =
            patients
                .flatMap { patient ->
                    gestationRepository
                        .getByPatient(patient.id)
                        .filter { it.isActiveGestation() }
                        .map { gestation -> GestationSummaryRow(patient, gestation) }
                }
        if (rows.isEmpty()) {
            return if (patients.isEmpty()) {
                null
            } else {
                "GESTATIONS: no active pregnancies found for the selected patients."
            }
        }
        val visible = rows.take(MAX_PATIENTS_SCANNED)
        val omitted = rows.size - visible.size
        val patientCount = rows.map { it.patient.id }.distinct().size
        return buildString {
            appendLine(
                "GESTATION TOTALS: ${rows.size} active ${pluralize("pregnancy", rows.size)} " +
                    "across $patientCount ${pluralize("patient", patientCount)}.",
            )
            appendLine("GESTATIONS:")
            visible.forEach { row -> appendLine(gestationLine(row.patient, row.gestation, today)) }
            if (omitted > 0) {
                appendLine("- GESTATION DETAILS TRUNCATED: $omitted more pregnancies are included in the total above.")
            }
        }.trimEnd()
    }

    private fun gestationLine(
        patient: Patient,
        gestation: Gestation,
        today: LocalDate,
    ): String {
        val progress = calculateGestationUseCase(gestation.breedingDate, today)
        return "- Gestation ${patient.name}: bred ${DateFormatting.formatHumanDate(gestation.breedingDate)}, " +
            "day ${progress.gestationDays}, " +
            "status ${gestation.status}, expected foaling ${progress.expectedDueDate}."
    }
}
