package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Read-only patient census tool. */
internal class PatientCensusTool(
    private val patientRepository: IPatientRepository,
    private val input: AnalysisToolSupport,
) {
    fun execute(call: RagToolCall): RagToolResult {
        val args = input.arguments(call)
        input.rejectUnknownKeys(args, emptySet())
        val patients = patientRepository.getPatientList()
        val returnedPatients = patients.take(AnalysisToolLimits.MAX_PATIENTS)
        val content =
            buildJsonObject {
                put("dataset", "patient_census")
                put("patient_count", patients.size)
                put("returned_patient_count", returnedPatients.size)
                put("truncated", patients.size > returnedPatients.size)
                put("patients", buildJsonArray { returnedPatients.forEach { add(patientJson(it)) } })
            }
        return analysisSuccess(call, content, returnedPatients.map(::patientSource))
    }

    private fun patientJson(patient: Patient): JsonObject =
        buildJsonObject {
            put("source", analysisSourceHeader(RecordType.Patient, patient.id))
            put("patient_id", patient.id)
            put("name", patient.name)
            put("species", patient.species)
            put("breed", patient.breed)
            put("gender", patient.gender)
            put("date_of_birth", patient.dateOfBirth?.toString())
        }

    private fun patientSource(patient: Patient): SearchResult =
        SearchResult(
            patientId = patient.id,
            patientName = patient.name,
            breed = patient.breed,
            microchipId = null,
            recordType = RecordType.Patient.wireName,
            recordId = patient.id,
            date = null,
            snippet = "Active patient; species ${patient.species}; breed ${patient.breed ?: "unknown"}.",
        )
}
