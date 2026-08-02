package com.github.rodrigotimoteo.animally.domain.export.pdf

import com.github.rodrigotimoteo.animally.domain.export.ExportBasicRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportClinicalRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportRecords
import com.github.rodrigotimoteo.animally.domain.export.ExportReproductiveRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.buildPdfSections
import com.github.rodrigotimoteo.animally.domain.export.filterByDate
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Gathers one patient's demographics and record tables into a [PdfReportData]
 * ready to be rendered by the platform [generatePdf] implementation.
 *
 * Reuses the CSV record gatherers, so both export formats stay consistent.
 */
@Single
class ExportPatientReportUseCase(
    @Provided private val patientRepository: IPatientRepository,
    @Provided private val basicRecords: ExportBasicRecordsUseCase,
    @Provided private val clinicalRecords: ExportClinicalRecordsUseCase,
    @Provided private val reproductiveRecords: ExportReproductiveRecordsUseCase,
) {
    /**
     * Builds the PDF report for the patient with the given [patientId].
     *
     * @param patientId the patient to export.
     * @param from inclusive lower date bound, or `null` for no lower bound.
     * @param to inclusive upper date bound, or `null` for no upper bound.
     * @throws IllegalArgumentException when the patient is not found.
     */
    operator fun invoke(
        patientId: Long,
        from: LocalDate?,
        to: LocalDate?,
    ): PdfReportData {
        val patient =
            requireNotNull(patientRepository.getPatientById(patientId)) {
                "Patient $patientId not found"
            }
        val records = gather(patientId).filterByDate(from, to)
        return PdfReportData(
            patient = PdfPatient.from(patient),
            sections = buildPdfSections(records),
            fromDate = from,
            toDate = to,
        )
    }

    private fun gather(patientId: Long): ExportRecords {
        val basic = basicRecords(patientId)
        val clinical = clinicalRecords(patientId)
        val reproductive = reproductiveRecords(patientId)
        return ExportRecords(
            anamnese = basic.anamnese,
            weights = basic.weights,
            consultations = basic.consultations,
            vaccinations = basic.vaccinations,
            dewormings = basic.dewormings,
            dentistries = basic.dentistries,
            lamenesses = clinical.lamenesses,
            surgeries = clinical.surgeries,
            medications = clinical.medications,
            labResults = clinical.labResults,
            imagings = clinical.imagings,
            farrierVisits = clinical.farrierVisits,
            reproductionEvents = reproductive.reproductionEvents,
            ultrasounds = reproductive.ultrasounds,
            gestations = reproductive.gestations,
            reproMedications = reproductive.reproMedications,
            controlledSubstances = reproductive.controlledSubstances,
        )
    }
}
