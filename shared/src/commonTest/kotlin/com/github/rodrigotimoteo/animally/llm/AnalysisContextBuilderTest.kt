package com.github.rodrigotimoteo.animally.llm

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalysisContextBuilderTest {
    private val repos = FakeAnalysisRepos()
    private val builder = repos.builder
    private val today = LocalDate(2025, 5, 11)

    // --- Intent gating ---

    @Test
    fun `given plain retrieval query when built then null`() {
        val summary = builder.build("Tell me about Thunder's colic history", today)

        assertNull(summary)
    }

    @Test
    fun `given census question when built then census block carries count and names`() {
        repos.patients.patients = listOf(testPatient(1, "Thunder"), testPatient(2, "Bella"))

        val summary = builder.build("How many patients do I have?", today)

        assertTrue(summary.orEmpty().contains(AnalysisContextBuilder.SUMMARY_HEADER))
        assertTrue(summary.orEmpty().contains("PATIENT CENSUS: 2 active patients: Thunder, Bella."))
    }

    @Test
    fun `given PT census question when built then census block emitted`() {
        repos.patients.patients = listOf(testPatient(1, "Thunder"))

        val summary = builder.build("Quantos pacientes tenho?", today)

        assertTrue(summary.orEmpty().contains("PATIENT CENSUS: 1 active patients: Thunder."))
    }

    // --- Weight trend math ---

    @Test
    fun `given multi-point weight series when built then min max latest and direction computed`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.weights.entries =
            listOf(
                testWeight(11, 1, kg = 510.0, date = LocalDate(2025, 2, 1)),
                testWeight(10, 1, kg = 500.0, date = LocalDate(2025, 1, 1)),
                testWeight(12, 1, kg = 505.0, date = LocalDate(2025, 3, 1)),
            )

        val summary = builder.build("What is Bella's weight trend?", today)

        val line = summary.orEmpty().lineSequence().first { it.startsWith("- Weight") }
        assertTrue(line.contains("min 500.0 kg (2025-01-01)"), line)
        assertTrue(line.contains("max 510.0 kg (2025-02-01)"), line)
        assertTrue(line.contains("latest 505.0 kg (2025-03-01)"), line)
        assertTrue(line.contains("- losing."), line)
    }

    @Test
    fun `given single weight entry when built then reported as single measurement`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.weights.entries = listOf(testWeight(10, 1, kg = 512.0, date = LocalDate(2025, 1, 15)))

        val summary = builder.build("What is Bella's weight trend?", today)

        val line = summary.orEmpty().lineSequence().first { it.startsWith("- Weight") }
        assertTrue(line.contains("single measurement 512.0 kg on 2025-01-15."), line)
    }

    @Test
    fun `given small latest gain when built then direction is stable`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.weights.entries =
            listOf(
                testWeight(10, 1, kg = 500.0, date = LocalDate(2025, 1, 1)),
                testWeight(11, 1, kg = 500.4, date = LocalDate(2025, 2, 1)),
            )

        val summary = builder.build("What is Bella's weight trend?", today)

        assertTrue(summary.orEmpty().contains("- stable."))
    }

    @Test
    fun `given weight question for unknown patient when built then null`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))

        val summary = builder.build("What is Storm's weight trend?", today)

        assertNull(summary)
    }

    // --- Care counts ---

    @Test
    fun `given care question when built then counts and last-done dates per patient`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"), testPatient(2, "Ghost"))
        repos.vaccinations.entries =
            listOf(
                testVaccination(21, 1, "Tetanus", administered = LocalDate(2024, 5, 1)),
                testVaccination(22, 1, "Flu", administered = LocalDate(2024, 9, 9)),
            )
        repos.farrierVisits.entries = listOf(testFarrierVisit(31, 1, date = LocalDate(2024, 6, 1)))

        val summary = builder.build("How many vaccinations has Bella had?", today)

        val text = summary.orEmpty()
        assertTrue(text.contains("CARE COUNTS:"), text)
        val line = text.lineSequence().first { it.startsWith("- Care Bella") }
        assertTrue(line.contains("2 vaccinations (last 2024-09-09)"), line)
        assertTrue(line.contains("0 dewormings"), line)
        assertTrue(line.contains("1 farrier visits (last 2024-06-01)"), line)
        assertFalse(text.contains("- Care Ghost"), "patients without records must be omitted")
    }

    // --- Gestation day count ---

    @Test
    fun `given active gestation when built then day count computed from breeding date`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.gestations.entries =
            listOf(
                testGestation(41, 1, breedingDate = LocalDate(2025, 1, 1), expectedDueDate = LocalDate(2025, 12, 6)),
                testGestation(42, 1, breedingDate = LocalDate(2024, 1, 1), expectedDueDate = LocalDate(2024, 12, 1), status = "Completed"),
            )

        val summary = builder.build("Which mares are pregnant?", today)

        val text = summary.orEmpty()
        assertTrue(text.contains("GESTATIONS:"), text)
        val line = text.lineSequence().first { it.startsWith("- Gestation") }
        // 2025-01-01 -> 2025-05-11 is exactly 130 days.
        assertTrue(line.contains("day 130,"), line)
        assertTrue(line.contains("status Active,"), line)
        assertFalse(text.contains("Completed"), "resolved gestations must be excluded")
    }

    // --- Overdue filter ---

    @Test
    fun `given overdue question when built then only items due before today listed`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.vaccinations.entries =
            listOf(
                testVaccination(21, 1, "Tetanus", administered = LocalDate(2024, 1, 1), nextDue = LocalDate(2025, 5, 10)),
                testVaccination(22, 1, "Flu", administered = LocalDate(2024, 1, 1), nextDue = LocalDate(2025, 5, 11)),
                testVaccination(23, 1, "EWV", administered = LocalDate(2024, 1, 1), nextDue = LocalDate(2025, 5, 12)),
            )
        repos.dewormings.entries =
            listOf(testDeworming(51, 1, "Ivermectin", administered = LocalDate(2024, 2, 1), nextDue = LocalDate(2025, 1, 31)))

        val summary = builder.build("Is any care overdue?", today)

        val text = summary.orEmpty()
        assertTrue(text.contains("OVERDUE CARE (due before 2025-05-11):"), text)
        assertTrue(text.contains("Tetanus was due 2025-05-10."), text)
        assertTrue(text.contains("Ivermectin was due 2025-01-31."), text)
        assertFalse(text.contains("due 2025-05-11"), "due-today is not overdue (strict <)")
        assertFalse(text.contains("EWV"), "future dues must not be listed")
    }

    @Test
    fun `given more overdue items than cap when built then list truncated`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.vaccinations.entries =
            (1..13).map { n ->
                testVaccination(n.toLong(), 1, "V$n", administered = LocalDate(2024, 1, 1), nextDue = LocalDate(2025, 1, n))
            }

        val summary = builder.build("Is any care overdue?", today)

        val lines = summary.orEmpty().lineSequence().count { it.startsWith("- OVERDUE") }
        assertEquals(12, lines)
    }

    @Test
    fun `given no overdue items when built then overdue block absent`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))

        val summary = builder.build("Is any care overdue?", today)

        assertNull(summary)
    }
}
