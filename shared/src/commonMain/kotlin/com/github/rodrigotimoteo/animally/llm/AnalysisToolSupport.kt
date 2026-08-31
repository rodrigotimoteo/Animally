package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** Shared validation and patient-scoping rules for the read-only tool handlers. */
internal class AnalysisToolSupport(
    private val patientRepository: IPatientRepository,
) {
    private val json = Json { explicitNulls = false }

    fun arguments(call: RagToolCall): JsonObject {
        val raw = call.arguments.trim().ifEmpty { "{}" }
        return runCatching { json.parseToJsonElement(raw) as? JsonObject }
            .getOrNull()
            ?: throw AnalysisToolInputException("Tool arguments must be a JSON object.")
    }

    fun rejectUnknownKeys(
        args: JsonObject,
        allowed: Set<String>,
    ) {
        val unknown = args.keys - allowed
        if (unknown.isNotEmpty()) {
            throw AnalysisToolInputException("Unsupported argument(s): ${unknown.sorted().joinToString()}.")
        }
    }

    fun dateRange(args: JsonObject): AnalysisDateRange {
        val from = args.optionalDate(AnalysisToolArguments.FROM_DATE)
        val to = args.optionalDate(AnalysisToolArguments.TO_DATE)
        if (from != null && to != null && from > to) {
            throw AnalysisToolInputException("from_date must be on or before to_date.")
        }
        return AnalysisDateRange(from, to)
    }

    fun matchingPatients(args: JsonObject): List<Patient> {
        val requestedName =
            args.optionalString(AnalysisToolArguments.PATIENT_NAME)
                ?: return patientRepository.getPatientList()
        val patients = patientRepository.getPatientList()
        val normalized = normalizePatientName(requestedName)
        val matches = patients.filter { normalizePatientName(it.name) == normalized }
        if (matches.size == 1) return matches
        if (matches.size > 1) {
            throw AnalysisToolInputException("More than one active patient is named $requestedName.")
        }
        val ambiguousTokenMatches =
            if (' ' !in normalized) {
                patients.filter { patient ->
                    normalizePatientName(patient.name).split(' ').contains(normalized)
                }
            } else {
                emptyList()
            }
        if (ambiguousTokenMatches.size > 1) {
            throw AnalysisToolInputException(
                "Patient name is ambiguous. Choose one of: ${ambiguousTokenMatches.joinToString { it.name }}.",
            )
        }
        // Tool arguments are untrusted model output. A unique prefix is not
        // enough to establish identity: "Ann" could silently select
        // "Annabelle" and contaminate an otherwise grounded answer.
        throw AnalysisToolInputException("No active patient matches $requestedName.")
    }
}

private fun normalizePatientName(value: String): String =
    value
        .trim()
        .split(Regex("\\s+"))
        .joinToString(" ")
        .lowercase()

internal fun JsonObject.optionalString(key: String): String? {
    val element = this[key] ?: return null
    val primitive =
        element as? JsonPrimitive
            ?: throw AnalysisToolInputException("$key must be a string.")
    if (!primitive.isString) {
        throw AnalysisToolInputException("$key must be a string.")
    }
    return primitive.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}

internal fun JsonObject.optionalDate(key: String): LocalDate? =
    optionalString(key)?.let { value ->
        runCatching { LocalDate.parse(value) }
            .getOrElse { throw AnalysisToolInputException("$key must use YYYY-MM-DD format.") }
    }

internal fun JsonObject.optionalBoolean(key: String): Boolean? {
    val element = this[key] ?: return null
    val primitive =
        element as? JsonPrimitive
            ?: throw AnalysisToolInputException("$key must be a boolean.")
    if (primitive.isString) {
        throw AnalysisToolInputException("$key must be a boolean.")
    }
    val value = primitive.contentOrNull?.lowercase()
    return when (value) {
        "true" -> true
        "false" -> false
        else -> throw AnalysisToolInputException("$key must be true or false.")
    }
}

internal fun analysisSuccess(
    call: RagToolCall,
    content: JsonObject,
    sources: List<SearchResult>,
): RagToolResult =
    RagToolResult(
        toolCallId = call.id,
        name = call.name,
        content = content.toString(),
        sources = sources,
    )

internal fun analysisErrorContent(message: String): String =
    buildJsonObject {
        put("error", message.take(AnalysisToolLimits.MAX_ERROR_CHARS))
    }.toString()

internal fun analysisSourceHeader(
    type: RecordType,
    id: Long,
): String = analysisSourceHeader(type.wireName, id)

internal fun analysisSourceHeader(
    type: String,
    id: Long,
): String = "[${type.uppercase()} #$id]"

internal data class AnalysisDateRange(
    val from: LocalDate?,
    val to: LocalDate?,
) {
    fun includes(date: LocalDate): Boolean = (from == null || date >= from) && (to == null || date <= to)
}

internal class AnalysisToolInputException(
    message: String,
) : IllegalArgumentException(message)

internal object AnalysisToolArguments {
    const val PATIENT_NAME = "patient_name"
    const val FROM_DATE = "from_date"
    const val TO_DATE = "to_date"
    const val RECORD_TYPE = "record_type"
    const val ACTIVE_ONLY = "active_only"
}

internal object AnalysisToolLimits {
    const val MAX_PATIENTS = 250
    const val MAX_DATA_ROWS = 500
    const val MAX_FIELD_CHARS = 160
    const val MAX_ERROR_CHARS = 240
    const val STABLE_WEIGHT_DELTA_KG = 0.5
    const val ROUNDING_FACTOR = 100.0

    val DATE_RANGE_KEYS =
        setOf(
            AnalysisToolArguments.PATIENT_NAME,
            AnalysisToolArguments.FROM_DATE,
            AnalysisToolArguments.TO_DATE,
        )
    val CARE_RECORD_TYPES = setOf("all", "vaccination", "deworming", "farrier")
    val CARE_WIRE_TYPES =
        listOf(
            RecordType.Vaccination.wireName,
            RecordType.Deworming.wireName,
            RecordType.FarrierVisit.wireName,
        )
}
