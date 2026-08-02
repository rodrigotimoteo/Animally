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
        return patients.joinToString(separator = "\n") { patient ->
            val records = filterByDate(gather(patient.id), from, to)
            csvExporter.exportPatientRecords(patient, records)
        }
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

    private fun filterByDate(
        records: ExportRecords,
        from: LocalDate?,
        to: LocalDate?,
    ): ExportRecords =
        ExportRecords(
            anamnese = records.anamnese,
            weights = records.weights.filter { inRange(it.date, from, to) },
            consultations = records.consultations.filter { inRange(it.date, from, to) },
            vaccinations = records.vaccinations.filter { inRange(it.dateAdministered, from, to) },
            dewormings = records.dewormings.filter { inRange(it.dateAdministered, from, to) },
            dentistries = records.dentistries.filter { inRange(it.date, from, to) },
            lamenesses = records.lamenesses.filter { inRange(it.date, from, to) },
            surgeries = records.surgeries.filter { inRange(it.date, from, to) },
            medications = records.medications.filter { it.startDate == null || inRange(it.startDate, from, to) },
            labResults = records.labResults.filter { inRange(it.date, from, to) },
            imagings = records.imagings.filter { inRange(it.date, from, to) },
            farrierVisits = records.farrierVisits.filter { inRange(it.date, from, to) },
            reproductionEvents = records.reproductionEvents.filter { inRange(it.date, from, to) },
            ultrasounds = records.ultrasounds.filter { inRange(it.date, from, to) },
            gestations = records.gestations.filter { inRange(it.breedingDate, from, to) },
            reproMedications = records.reproMedications.filter { inRange(it.dateAdministered, from, to) },
            controlledSubstances = records.controlledSubstances.filter { inRange(it.date, from, to) },
        )

    private fun inRange(
        date: LocalDate,
        from: LocalDate?,
        to: LocalDate?,
    ): Boolean = (from == null || date >= from) && (to == null || date <= to)
}
