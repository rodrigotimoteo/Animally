package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.round
import kotlin.time.Clock

/** Read-only deterministic weight statistics tool. */
internal class WeightSummaryTool(
    private val weightRepository: IWeightRepository,
    private val input: AnalysisToolSupport,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    fun execute(call: RagToolCall): RagToolResult {
        val args = input.arguments(call)
        input.rejectUnknownKeys(args, AnalysisToolLimits.DATE_RANGE_KEYS)
        val range = input.dateRange(args)
        val patients = input.matchingPatients(args)
        val rows = loadRows(patients, range)
        val returnedRows = rows.take(AnalysisToolLimits.MAX_DATA_ROWS)
        val summaryPatients = patients.take(AnalysisToolLimits.MAX_PATIENTS)
        val rowsByPatient = rows.groupBy { it.patient.id }
        val content =
            buildContent(
                WeightContent(
                    args = args,
                    range = range,
                    patients = patients,
                    rows = rows,
                    returnedRows = returnedRows,
                    summaryPatients = summaryPatients,
                    rowsByPatient = rowsByPatient,
                ),
            )
        val boundaryRows =
            summaryPatients.flatMap { patient ->
                rowsByPatient[patient.id].orEmpty().let { patientRows ->
                    listOfNotNull(
                        patientRows.minByOrNull { it.weight.date },
                        patientRows.maxByOrNull { it.weight.date },
                    )
                }
            }
        val sourceRows = (returnedRows + boundaryRows).distinctBy { it.patient.id to it.weight.id }
        return analysisSuccess(call, content, sourceRows.map(::weightSource))
    }

    private fun loadRows(
        patients: List<Patient>,
        range: AnalysisDateRange,
    ): List<WeightRow> =
        patients
            .flatMap { patient ->
                weightRepository
                    .getByPatient(patient.id)
                    .filter { weight -> range.includes(weight.date) }
                    .map { weight -> WeightRow(patient, weight) }
            }.sortedWith(compareBy({ it.weight.date }, { it.patient.name.lowercase() }, { it.weight.id }))

    private fun buildContent(data: WeightContent): JsonObject =
        buildJsonObject {
            put("dataset", "weights")
            put("as_of", todayProvider().toString())
            put("patient_filter", data.args.optionalString(AnalysisToolArguments.PATIENT_NAME))
            put("from_date", data.range.from?.toString())
            put("to_date", data.range.to?.toString())
            put("measurement_count", data.rows.size)
            put("patient_count", data.patients.size)
            put(
                "patients_with_measurements",
                data.rows
                    .map { it.patient.id }
                    .distinct()
                    .size,
            )
            put("returned_patient_count", data.summaryPatients.size)
            put("patient_list_truncated", data.patients.size > data.summaryPatients.size)
            put("truncated", data.rows.size > data.returnedRows.size)
            put(
                "by_patient",
                buildJsonArray {
                    data.summaryPatients.forEach { patient ->
                        add(weightPatientSummary(patient, data.rowsByPatient[patient.id].orEmpty()))
                    }
                },
            )
            put("measurements", buildJsonArray { data.returnedRows.forEach { add(weightJson(it)) } })
        }

    private fun weightPatientSummary(
        patient: Patient,
        rows: List<WeightRow>,
    ): JsonObject {
        val measurements = rows.sortedBy(WeightRow::date)
        return buildJsonObject {
            put("patient_name", patient.name)
            put("measurement_count", measurements.size)
            if (measurements.isNotEmpty()) {
                val values = measurements.map { it.weight.weightKg }
                val first = measurements.first()
                val latest = measurements.last()
                val change = latest.weight.weightKg - first.weight.weightKg
                put("average_kg", values.average().rounded())
                put("minimum_kg", values.minOrNull())
                put("maximum_kg", values.maxOrNull())
                put("median_kg", median(values).rounded())
                put("first", weightJson(first))
                put("latest", weightJson(latest))
                put("change_kg", change.rounded())
                put("direction", weightDirection(change, measurements.size))
            }
        }
    }

    private fun weightJson(row: WeightRow): JsonObject =
        buildJsonObject {
            put("source", analysisSourceHeader(RecordType.Weight, row.weight.id))
            put("record_id", row.weight.id)
            put("patient_name", row.patient.name)
            put("breed", row.patient.breed)
            put("date", row.weight.date.toString())
            put("weight_kg", row.weight.weightKg)
        }

    private fun weightSource(row: WeightRow): SearchResult =
        SearchResult(
            patientId = row.patient.id,
            patientName = row.patient.name,
            breed = row.patient.breed,
            microchipId = null,
            recordType = RecordType.Weight.wireName,
            recordId = row.weight.id,
            date = row.weight.date,
            snippet = "Weight ${row.weight.weightKg} kg.",
        )

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun weightDirection(
        change: Double,
        measurementCount: Int,
    ): String =
        when {
            measurementCount < 2 -> "insufficient_data"
            change > AnalysisToolLimits.STABLE_WEIGHT_DELTA_KG -> "gaining"
            change < -AnalysisToolLimits.STABLE_WEIGHT_DELTA_KG -> "losing"
            else -> "stable"
        }

    private fun Double.rounded(): Double = round(this * ROUNDING_FACTOR) / ROUNDING_FACTOR

    private data class WeightContent(
        val args: JsonObject,
        val range: AnalysisDateRange,
        val patients: List<Patient>,
        val rows: List<WeightRow>,
        val returnedRows: List<WeightRow>,
        val summaryPatients: List<Patient>,
        val rowsByPatient: Map<Long, List<WeightRow>>,
    )

    private data class WeightRow(
        val patient: Patient,
        val weight: Weight,
    ) {
        val date: LocalDate get() = weight.date
    }

    private companion object {
        const val ROUNDING_FACTOR = AnalysisToolLimits.ROUNDING_FACTOR
    }
}
