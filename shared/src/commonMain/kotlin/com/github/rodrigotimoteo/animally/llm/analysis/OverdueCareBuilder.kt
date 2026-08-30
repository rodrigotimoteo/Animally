package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import kotlinx.datetime.LocalDate

internal class OverdueCareBuilder(
    private val vaccinationRepository: IVaccinationRepository,
    private val dewormingRepository: IDewormingRepository,
    private val farrierVisitRepository: IFarrierVisitRepository,
    private val gestationRepository: IGestationRepository,
    private val calculateGestationUseCase: CalculateGestationUseCase,
) {
    fun overdueBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val allLines = patients.flatMap { overdueLinesForPatient(it, today) }
        if (allLines.isEmpty()) {
            return if (patients.isEmpty()) null else "OVERDUE CARE (due before $today): no overdue care found."
        }
        val lines = allLines.take(MAX_OVERDUE_ITEMS)
        return buildString {
            appendLine("OVERDUE CARE (due before $today):")
            appendLine(lines.joinToString("\n"))
            val omitted = allLines.size - lines.size
            if (omitted > 0) {
                appendLine("- OVERDUE DETAILS TRUNCATED: $omitted more overdue items are included in the total above.")
            }
        }.trimEnd()
    }

    private fun overdueLinesForPatient(
        patient: Patient,
        today: LocalDate,
    ): List<String> =
        buildList {
            vaccinationRepository.getByPatient(patient.id).forEach { vaccination ->
                vaccination.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Vaccination ${vaccination.vaccineName} was due $due.")
                }
            }
            dewormingRepository.getByPatient(patient.id).forEach { deworming ->
                deworming.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Deworming ${deworming.product} was due $due.")
                }
            }
            farrierVisitRepository.getByPatient(patient.id).forEach { visit ->
                visit.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Farrier visit was due $due.")
                }
            }
            gestationRepository.getByPatient(patient.id).filter { it.isActiveGestation() }.forEach { gestation ->
                val dueDate = calculateGestationUseCase(gestation.breedingDate, today).expectedDueDate
                dueDate.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Expected foaling was due $due.")
                }
            }
        }
}
