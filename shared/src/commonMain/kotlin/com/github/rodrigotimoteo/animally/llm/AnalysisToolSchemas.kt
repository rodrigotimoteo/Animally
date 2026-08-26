package com.github.rodrigotimoteo.animally.llm

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Native tool schemas for the first read-only analysis surface. */
internal object AnalysisToolSchemas {
    private val dateRangeProperties =
        buildJsonObject {
            put("patient_name", stringProperty("Optional horse name or unambiguous name prefix."))
            put("from_date", stringProperty("Optional inclusive start date in YYYY-MM-DD format."))
            put("to_date", stringProperty("Optional inclusive end date in YYYY-MM-DD format."))
        }

    private val careProperties =
        buildJsonObject {
            dateRangeProperties.forEach { (key, value) -> put(key, value) }
            put(
                "record_type",
                buildJsonObject {
                    put("type", "string")
                    put(
                        "enum",
                        buildJsonArray {
                            add(JsonPrimitive("all"))
                            add(JsonPrimitive("vaccination"))
                            add(JsonPrimitive("deworming"))
                            add(JsonPrimitive("farrier"))
                        },
                    )
                    put("description", "Optional care type filter; defaults to all.")
                },
            )
        }

    private val gestationProperties =
        buildJsonObject {
            put("patient_name", stringProperty("Optional horse name or unambiguous name prefix."))
            put(
                "active_only",
                buildJsonObject {
                    put("type", "boolean")
                    put("description", "Defaults to true; set false to include resolved history.")
                },
            )
        }

    val definitions: List<RagToolDefinition> =
        listOf(
            RagToolDefinition(
                name = AnalysisToolNames.PATIENT_CENSUS,
                description = CENSUS_DESCRIPTION,
                parameters = objectSchema(buildJsonObject {}),
            ),
            RagToolDefinition(
                name = AnalysisToolNames.WEIGHT_SUMMARY,
                description = WEIGHT_DESCRIPTION,
                parameters = weightSchema(),
            ),
            RagToolDefinition(
                name = AnalysisToolNames.CARE_SUMMARY,
                description = CARE_DESCRIPTION,
                parameters = objectSchema(careProperties),
            ),
            RagToolDefinition(
                name = AnalysisToolNames.GESTATION_SUMMARY,
                description = GESTATION_DESCRIPTION,
                parameters = objectSchema(gestationProperties),
            ),
        )

    private fun stringProperty(description: String): JsonObject =
        buildJsonObject {
            put("type", "string")
            put("description", description)
        }

    private fun weightSchema(): JsonObject =
        objectSchema(
            properties = dateRangeProperties,
            description = "Filter the active weight measurements by horse and/or inclusive dates.",
        )

    private fun objectSchema(
        properties: JsonObject,
        description: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("type", "object")
            description?.let { put("description", it) }
            put("properties", properties)
            put("additionalProperties", false)
        }

    private const val CENSUS_DESCRIPTION =
        "Read-only count and compact list of active horses. Use this for census, patient counts, " +
            "or questions about the current patient population."
    private const val WEIGHT_DESCRIPTION =
        "Read-only weight dataset analysis. Returns per-horse count, average, minimum, maximum, " +
            "median, first/latest measurements, change, and bounded raw measurements for comparisons and trends."
    private const val CARE_DESCRIPTION =
        "Read-only preventive-care analysis across vaccinations, dewormings, and farrier visits. " +
            "Returns counts by type and horse, due dates, and bounded record rows. It never returns medication dosages."
    private const val GESTATION_DESCRIPTION =
        "Read-only reproductive analysis of gestation records. Returns status, " +
            "live days since breeding, recomputed expected foaling date, and bounded " +
            "records. Use active_only=false when historical gestations matter."
}

internal object AnalysisToolNames {
    const val PATIENT_CENSUS = "patient_census"
    const val WEIGHT_SUMMARY = "weight_summary"
    const val CARE_SUMMARY = "care_summary"
    const val GESTATION_SUMMARY = "gestation_summary"
}
