package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalysisToolRegistryTest {
    private val repos = FakeAnalysisRepos()
    private val today = LocalDate(2025, 5, 11)
    private val registry =
        AnalysisToolRegistry(
            patientRepository = repos.patients,
            weightRepository = repos.weights,
            vaccinationRepository = repos.vaccinations,
            dewormingRepository = repos.dewormings,
            farrierVisitRepository = repos.farrierVisits,
            gestationRepository = repos.gestations,
            calculateGestationUseCase = CalculateGestationUseCase(),
            todayProvider = { today },
        )

    @Test
    fun `definitions expose only the four read-only analysis tools`() {
        assertEquals(
            setOf(
                AnalysisToolNames.PATIENT_CENSUS,
                AnalysisToolNames.WEIGHT_SUMMARY,
                AnalysisToolNames.CARE_SUMMARY,
                AnalysisToolNames.GESTATION_SUMMARY,
            ),
            registry.definitions.map(RagToolDefinition::name).toSet(),
        )
        registry.definitions.forEach { definition ->
            assertEquals(false, definition.parameters["additionalProperties"]?.jsonPrimitive?.boolean)
        }
    }

    @Test
    fun `weight summary computes deterministic statistics and applies date filter`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.weights.entries =
                listOf(
                    testWeight(10, 1, 500.0, LocalDate(2025, 1, 1)),
                    testWeight(11, 1, 510.0, LocalDate(2025, 2, 1)),
                    testWeight(12, 1, 520.0, LocalDate(2025, 3, 1)),
                )

            val result =
                registry.execute(
                    RagToolCall(
                        id = "call-weight",
                        name = AnalysisToolNames.WEIGHT_SUMMARY,
                        arguments = """{"patient_name":"Bella","from_date":"2025-01-01","to_date":"2025-02-28"}""",
                    ),
                )
            val json = Json.parseToJsonElement(result.content).jsonObject
            val summary = json["by_patient"]!!.jsonArray.single().jsonObject

            assertFalse(result.isError, result.content)
            assertEquals(2, json["measurement_count"]!!.jsonPrimitive.int)
            assertEquals(505.0, summary["average_kg"]!!.jsonPrimitive.double)
            assertEquals(500.0, summary["minimum_kg"]!!.jsonPrimitive.double)
            assertEquals(510.0, summary["maximum_kg"]!!.jsonPrimitive.double)
            assertEquals(10.0, summary["change_kg"]!!.jsonPrimitive.double)
            assertEquals("gaining", summary["direction"]!!.jsonPrimitive.contentOrNull)
            assertEquals(
                "[WEIGHT #10]",
                json["measurements"]!!
                    .jsonArray
                    .first()
                    .jsonObject["source"]!!
                    .jsonPrimitive.contentOrNull,
            )
            assertEquals(listOf("WEIGHT"), result.sources.map { it.recordType }.distinct())
        }

    @Test
    fun `care summary counts record types without exposing medication fields`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.vaccinations.entries = listOf(testVaccination(21, 1, "Tetanus", LocalDate(2025, 1, 1)))
            repos.dewormings.entries = listOf(testDeworming(31, 1, "Ivermectin", LocalDate(2025, 2, 1)))
            repos.farrierVisits.entries = listOf(testFarrierVisit(41, 1, LocalDate(2025, 3, 1)))

            val result =
                registry.execute(
                    RagToolCall(
                        id = "call-care",
                        name = AnalysisToolNames.CARE_SUMMARY,
                        arguments = """{"record_type":"all"}""",
                    ),
                )
            val json = Json.parseToJsonElement(result.content).jsonObject
            val counts = json["counts_by_type"]!!.jsonObject

            assertFalse(result.isError, result.content)
            assertEquals(3, json["record_count"]!!.jsonPrimitive.int)
            assertEquals(1, counts["VACCINATION"]!!.jsonPrimitive.int)
            assertEquals(1, counts["DEWORMING"]!!.jsonPrimitive.int)
            assertEquals(1, counts["FARRIER_VISIT"]!!.jsonPrimitive.int)
            assertTrue("dosage" !in result.content.lowercase())
            assertEquals(3, result.sources.size)
        }

    @Test
    fun `gestation summary calculates live day count and excludes resolved by default`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.gestations.entries =
                listOf(
                    testGestation(51, 1, LocalDate(2025, 1, 1), LocalDate(2025, 12, 6)),
                    testGestation(52, 1, LocalDate(2024, 1, 1), LocalDate(2024, 12, 1), status = "Completed"),
                )

            val active =
                registry.execute(
                    RagToolCall("call-gestation", AnalysisToolNames.GESTATION_SUMMARY, "{}"),
                )
            val activeJson = Json.parseToJsonElement(active.content).jsonObject
            val activeRecord = activeJson["records"]!!.jsonArray.single().jsonObject

            assertFalse(active.isError, active.content)
            assertEquals(1, activeJson["record_count"]!!.jsonPrimitive.int)
            assertEquals(130, activeRecord["gestation_days"]!!.jsonPrimitive.int)
            assertEquals("2025-12-07", activeRecord["expected_due_date"]!!.jsonPrimitive.contentOrNull)

            val history =
                registry.execute(
                    RagToolCall("call-history", AnalysisToolNames.GESTATION_SUMMARY, """{"active_only":false}"""),
                )
            assertEquals(
                2,
                Json
                    .parseToJsonElement(history.content)
                    .jsonObject["record_count"]!!
                    .jsonPrimitive.int,
            )
        }

    @Test
    fun `invalid arguments and ambiguous patient names return safe tool errors`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella North"), testPatient(2, "Bella South"))

            val ambiguous =
                registry.execute(
                    RagToolCall("call-ambiguous", AnalysisToolNames.WEIGHT_SUMMARY, """{"patient_name":"Bella"}"""),
                )
            val invalidDate =
                registry.execute(
                    RagToolCall("call-date", AnalysisToolNames.WEIGHT_SUMMARY, """{"from_date":"tomorrow"}"""),
                )
            val unknown = registry.execute(RagToolCall("call-unknown", "not_a_tool", "{}"))

            assertTrue(ambiguous.isError)
            assertTrue(ambiguous.content.contains("ambiguous"))
            assertTrue(invalidDate.isError)
            assertTrue(invalidDate.content.contains("YYYY-MM-DD"))
            assertTrue(unknown.isError)
            assertTrue(unknown.content.contains("Unknown analysis tool"))
        }

    @Test
    fun `unique patient prefixes return a safe error instead of selecting another patient`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Annabelle"))

            val result =
                registry.execute(
                    RagToolCall("call-prefix", AnalysisToolNames.WEIGHT_SUMMARY, """{"patient_name":"Ann"}"""),
                )

            assertTrue(result.isError)
            assertTrue(result.content.contains("No active patient matches Ann"))
        }

    @Test
    fun `scoped tool calls fail closed when patient name is omitted or mismatched`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella"), testPatient(2, "Shadow"))
            val scope = RagToolExecutionScope(resolvedPatientId = 1L, requiresPatientName = true)

            val omitted =
                registry.execute(
                    RagToolCall(
                        "call-omitted",
                        AnalysisToolNames.WEIGHT_SUMMARY,
                        "{}",
                        executionScope = scope,
                    ),
                )
            val mismatched =
                registry.execute(
                    RagToolCall(
                        "call-mismatched",
                        AnalysisToolNames.CARE_SUMMARY,
                        """{"patient_name":"Shadow"}""",
                        executionScope = scope,
                    ),
                )
            val censusOmitted =
                registry.execute(
                    RagToolCall(
                        "call-census-omitted",
                        AnalysisToolNames.PATIENT_CENSUS,
                        "{}",
                        executionScope = scope,
                    ),
                )

            assertTrue(omitted.isError)
            assertTrue(omitted.content.contains("patient_name is required"))
            assertTrue(mismatched.isError)
            assertTrue(mismatched.content.contains("does not match"))
            assertTrue(censusOmitted.isError)
        }

    @Test
    fun `population tool calls retain optional patient filtering behavior`() =
        runTest {
            repos.patients.patients = listOf(testPatient(1, "Bella"), testPatient(2, "Shadow"))
            repos.weights.entries =
                listOf(
                    testWeight(10, 1, 500.0, LocalDate(2025, 1, 1)),
                    testWeight(11, 2, 510.0, LocalDate(2025, 1, 1)),
                )

            val result =
                registry.execute(
                    RagToolCall("call-population", AnalysisToolNames.WEIGHT_SUMMARY, "{}"),
                )
            val json = Json.parseToJsonElement(result.content).jsonObject

            assertFalse(result.isError, result.content)
            assertEquals(2, json["patient_count"]!!.jsonPrimitive.int)
            assertEquals(2, json["measurement_count"]!!.jsonPrimitive.int)
        }
}
