package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.model.isActiveGestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Read-only reproductive history tool with live gestation calculations. */
internal class GestationSummaryTool(
    private val gestationRepository: IGestationRepository,
    private val calculateGestationUseCase: CalculateGestationUseCase,
    private val input: AnalysisToolSupport,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    fun execute(call: RagToolCall): RagToolResult {
        val args = input.arguments(call)
        input.rejectUnknownKeys(args, setOf(AnalysisToolArguments.PATIENT_NAME, AnalysisToolArguments.ACTIVE_ONLY))
        val activeOnly = args.optionalBoolean(AnalysisToolArguments.ACTIVE_ONLY) ?: true
        val patients = input.matchingPatients(args, call.executionScope)
        val today = todayProvider()
        val rows = loadRows(patients, today, activeOnly)
        val returnedRows = rows.take(AnalysisToolLimits.MAX_DATA_ROWS)
        val content =
            buildContent(
                GestationContent(
                    args = args,
                    today = today,
                    activeOnly = activeOnly,
                    patients = patients,
                    rows = rows,
                    returnedRows = returnedRows,
                ),
            )
        return analysisSuccess(call, content, returnedRows.map(::gestationSource))
    }

    private fun loadRows(
        patients: List<Patient>,
        today: LocalDate,
        activeOnly: Boolean,
    ): List<GestationRow> =
        patients
            .flatMap { patient ->
                gestationRepository
                    .getByPatient(patient.id)
                    .map { gestation ->
                        GestationRow(
                            patient = patient,
                            gestation = calculateGestationUseCase.withCurrentProgress(gestation, today),
                        )
                    }
            }.filter { row -> !activeOnly || row.gestation.isActiveGestation() }
            .sortedWith(compareBy({ it.gestation.breedingDate }, { it.patient.name.lowercase() }, { it.gestation.id }))

    private fun buildContent(data: GestationContent): JsonObject =
        buildJsonObject {
            put("dataset", "gestations")
            put("as_of", data.today.toString())
            put("patient_filter", data.args.optionalString(AnalysisToolArguments.PATIENT_NAME))
            put("active_only", data.activeOnly)
            put("record_count", data.rows.size)
            put("active_count", data.rows.count { it.gestation.isActiveGestation() })
            put("patient_count", data.patients.size)
            put(
                "patients_with_records",
                data.rows
                    .map { it.patient.id }
                    .distinct()
                    .size,
            )
            put("returned_patient_count", data.patients.size.coerceAtMost(AnalysisToolLimits.MAX_PATIENTS))
            put("patient_list_truncated", data.patients.size > AnalysisToolLimits.MAX_PATIENTS)
            put("truncated", data.rows.size > data.returnedRows.size)
            put("counts_by_status", countsByStatus(data.rows))
            put("records", buildJsonArray { data.returnedRows.forEach { add(gestationJson(it)) } })
        }

    private fun countsByStatus(rows: List<GestationRow>): JsonObject =
        buildJsonObject {
            rows.groupingBy { it.gestation.status }.eachCount().forEach { (status, count) ->
                put(status, count)
            }
        }

    private fun gestationJson(row: GestationRow): JsonObject =
        buildJsonObject {
            put("source", analysisSourceHeader(RecordType.Gestation, row.gestation.id))
            put("record_id", row.gestation.id)
            put("patient_name", row.patient.name)
            put("breed", row.patient.breed)
            put("breeding_date", row.gestation.breedingDate.toString())
            put("expected_due_date", row.gestation.expectedDueDate.toString())
            put("gestation_days", row.gestation.gestationDays)
            put("status", row.gestation.status)
            put("fetal_count", row.gestation.fetalCount)
            put("last_check_date", row.gestation.lastCheckDate?.toString())
        }

    private fun gestationSource(row: GestationRow): SearchResult =
        SearchResult(
            patientId = row.patient.id,
            patientName = row.patient.name,
            breed = row.patient.breed,
            microchipId = null,
            recordType = RecordType.Gestation.wireName,
            recordId = row.gestation.id,
            date = row.gestation.breedingDate,
            snippet = "${row.gestation.status}; gestation day ${row.gestation.gestationDays}.",
        )

    private data class GestationContent(
        val args: JsonObject,
        val today: LocalDate,
        val activeOnly: Boolean,
        val patients: List<Patient>,
        val rows: List<GestationRow>,
        val returnedRows: List<GestationRow>,
    )

    private data class GestationRow(
        val patient: Patient,
        val gestation: Gestation,
    )
}
