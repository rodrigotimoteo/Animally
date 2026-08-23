package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Exports patient records as a CSV document.
 *
 * When [patientId] is `null`, every active patient is exported, one block per
 * patient. Records carrying a date field are filtered to the inclusive
 * `from`/`to` date range; entities without a meaningful date field
 * (patient, anamnese) are always included.
 */
@Single
class ExportCsvUseCase(
    @Provided private val patientRepository: IPatientRepository,
    @Provided private val basicRecords: ExportBasicRecordsUseCase,
    @Provided private val clinicalRecords: ExportClinicalRecordsUseCase,
    @Provided private val reproductiveRecords: ExportReproductiveRecordsUseCase,
    @Provided private val csvExporter: CsvExporter,
) {
    /**
     * Generates the CSV document.
     *
     * @param patientId the patient to export, or `null` to export all patients.
     * @param from inclusive lower date bound, or `null` for no lower bound.
     * @param to inclusive upper date bound, or `null` for no upper bound.
     * @return the CSV document, or an empty string when no patients match.
     */
    operator fun invoke(
        patientId: Long?,
        from: LocalDate?,
        to: LocalDate?,
    ): String {
        val patients =
            if (patientId != null) {
                listOfNotNull(patientRepository.getPatientById(patientId))
            } else {
                patientRepository.getPatientList()
            }
        if (patients.isEmpty()) return ""
        val document =
            patients.joinToString(separator = "\n") { patient ->
                val records = gather(patient.id).filterByDate(from, to)
                csvExporter.exportPatientRecords(patient, records)
            }
        return CsvFormatter.UTF8_BOM + document
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
