package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Read-only preventive-care counts and due-date tool. */
internal class CareSummaryTool(
    private val vaccinationRepository: IVaccinationRepository,
    private val dewormingRepository: IDewormingRepository,
    private val farrierVisitRepository: IFarrierVisitRepository,
    private val input: AnalysisToolSupport,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    fun execute(call: RagToolCall): RagToolResult {
        val args = input.arguments(call)
        input.rejectUnknownKeys(
            args,
            AnalysisToolLimits.DATE_RANGE_KEYS + AnalysisToolArguments.RECORD_TYPE,
        )
        val range = input.dateRange(args)
        val recordType = args.optionalString(AnalysisToolArguments.RECORD_TYPE)?.lowercase() ?: CARE_ALL
        if (recordType !in AnalysisToolLimits.CARE_RECORD_TYPES) {
            throw AnalysisToolInputException("record_type must be all, vaccination, deworming, or farrier.")
        }
        val patients = input.matchingPatients(args)
        val rows = loadRows(patients, recordType, range)
        val returnedRows = rows.take(AnalysisToolLimits.MAX_DATA_ROWS)
        val summaryPatients = patients.take(AnalysisToolLimits.MAX_PATIENTS)
        val content =
            buildContent(
                CareContent(
                    args = args,
                    range = range,
                    recordType = recordType,
                    patients = patients,
                    rows = rows,
                    returnedRows = returnedRows,
                    summaryPatients = summaryPatients,
                ),
            )
        return analysisSuccess(call, content, returnedRows.map(::careSource))
    }

    private fun loadRows(
        patients: List<Patient>,
        recordType: String,
        range: AnalysisDateRange,
    ): List<CareRow> =
        patients
            .flatMap { patient -> careRows(patient, recordType) }
            .filter { row -> range.includes(row.date) }
            .sortedWith(compareBy({ it.date }, { it.patient.name.lowercase() }, { it.recordId }))

    private fun buildContent(data: CareContent): JsonObject {
        val rowsByPatient = data.rows.groupBy { it.patient.id }
        return buildJsonObject {
            put("dataset", "preventive_care")
            put("as_of", todayProvider().toString())
            put("patient_filter", data.args.optionalString(AnalysisToolArguments.PATIENT_NAME))
            put("record_type", data.recordType)
            put("from_date", data.range.from?.toString())
            put("to_date", data.range.to?.toString())
            put("record_count", data.rows.size)
            put("patient_count", data.patients.size)
            put(
                "patients_with_records",
                data.rows
                    .map { it.patient.id }
                    .distinct()
                    .size,
            )
            put("returned_patient_count", data.summaryPatients.size)
            put("patient_list_truncated", data.patients.size > data.summaryPatients.size)
            put("truncated", data.rows.size > data.returnedRows.size)
            put("counts_by_type", countsByType(data.rows))
            put(
                "by_patient",
                buildJsonArray {
                    data.summaryPatients.forEach { patient ->
                        add(patientSummary(patient, rowsByPatient[patient.id].orEmpty()))
                    }
                },
            )
            put("records", buildJsonArray { data.returnedRows.forEach { add(careJson(it)) } })
        }
    }

    private fun careRows(
        patient: Patient,
        requestedType: String,
    ): List<CareRow> =
        buildList {
            if (requestedType == CARE_ALL || requestedType == CARE_VACCINATION) {
                vaccinationRepository.getByPatient(patient.id).forEach { vaccination ->
                    add(
                        CareRow(
                            patient = patient,
                            recordType = RecordType.Vaccination.wireName,
                            recordId = vaccination.id,
                            date = vaccination.dateAdministered,
                            label = vaccination.vaccineName,
                            nextDueDate = vaccination.nextDueDate,
                        ),
                    )
                }
            }
            if (requestedType == CARE_ALL || requestedType == CARE_DEWORMING) {
                dewormingRepository.getByPatient(patient.id).forEach { deworming ->
                    add(
                        CareRow(
                            patient = patient,
                            recordType = RecordType.Deworming.wireName,
                            recordId = deworming.id,
                            date = deworming.dateAdministered,
                            label = deworming.product,
                            nextDueDate = deworming.nextDueDate,
                        ),
                    )
                }
            }
            if (requestedType == CARE_ALL || requestedType == CARE_FARRIER) {
                farrierVisitRepository.getByPatient(patient.id).forEach { visit ->
                    add(
                        CareRow(
                            patient = patient,
                            recordType = RecordType.FarrierVisit.wireName,
                            recordId = visit.id,
                            date = visit.date,
                            label = visit.trimOrShoe ?: visit.shoeType ?: "Farrier visit",
                            nextDueDate = visit.nextDueDate,
                        ),
                    )
                }
            }
        }

    private fun countsByType(rows: List<CareRow>): JsonObject =
        buildJsonObject {
            AnalysisToolLimits.CARE_WIRE_TYPES.forEach { type ->
                put(type, rows.count { it.recordType == type })
            }
        }

    private fun patientSummary(
        patient: Patient,
        rows: List<CareRow>,
    ): JsonObject =
        buildJsonObject {
            put("patient_name", patient.name)
            put("record_count", rows.size)
            put("counts_by_type", countsByType(rows))
        }

    private fun careJson(row: CareRow): JsonObject =
        buildJsonObject {
            put("source", analysisSourceHeader(row.recordType, row.recordId))
            put("record_id", row.recordId)
            put("record_type", row.recordType)
            put("patient_name", row.patient.name)
            put("breed", row.patient.breed)
            put("date", row.date.toString())
            put("label", row.label.take(AnalysisToolLimits.MAX_FIELD_CHARS))
            put("next_due_date", row.nextDueDate?.toString())
        }

    private fun careSource(row: CareRow): SearchResult =
        SearchResult(
            patientId = row.patient.id,
            patientName = row.patient.name,
            breed = row.patient.breed,
            microchipId = null,
            recordType = row.recordType,
            recordId = row.recordId,
            date = row.date,
            snippet = careSnippet(row),
        )

    private fun careSnippet(row: CareRow): String =
        "${row.label.take(AnalysisToolLimits.MAX_FIELD_CHARS)}; " +
            "next due ${row.nextDueDate ?: "not recorded"}."

    private data class CareRow(
        val patient: Patient,
        val recordType: String,
        val recordId: Long,
        val date: LocalDate,
        val label: String,
        val nextDueDate: LocalDate?,
    )

    private data class CareContent(
        val args: JsonObject,
        val range: AnalysisDateRange,
        val recordType: String,
        val patients: List<Patient>,
        val rows: List<CareRow>,
        val returnedRows: List<CareRow>,
        val summaryPatients: List<Patient>,
    )

    private companion object {
        const val CARE_ALL = "all"
        const val CARE_VACCINATION = "vaccination"
        const val CARE_DEWORMING = "deworming"
        const val CARE_FARRIER = "farrier"
    }
}
