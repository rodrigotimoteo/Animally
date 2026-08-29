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

        assertTrue(summary.orEmpty().contains("PATIENT CENSUS: 1 active patient: Thunder."))
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
        assertTrue(line.contains("single measurement 512.0 kg on 15 Jan 2025."), line)
    }

    @Test
    fun `given a population weight question then totals include every patient and detail cap is explicit`() {
        repos.patients.patients = (1L..12L).map { id -> testPatient(id, "Horse $id") }
        repos.weights.entries = (1L..12L).map { id -> testWeight(id, id, 500.0 + id, LocalDate(2025, 1, id.toInt())) }

        val summary = builder.build("Analyze the average weight in my dataset", today).orEmpty()

        assertTrue(summary.contains("WEIGHT SUMMARY: 12 measurements across 12 patients"), summary)
        assertTrue(summary.contains("average 506.5 kg"), summary)
        assertTrue(summary.contains("WEIGHT DETAILS TRUNCATED: 2"), summary)
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

    @Test
    fun `given a patient name prefix when built then a longer name is not selected`() {
        repos.patients.patients = listOf(testPatient(1, "Annabelle"))
        repos.weights.entries = listOf(testWeight(10, 1, kg = 512.0, date = LocalDate(2025, 1, 15)))

        val summary = builder.build("What is Ann's weight trend?", today)

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
        assertTrue(line.contains("2 vaccinations (last 9 Sep 2024)"), line)
        assertTrue(line.contains("0 dewormings"), line)
        assertTrue(line.contains("1 farrier visits (last 1 Jun 2024)"), line)
        // Humanized on purpose: raw ISO strings in the authoritative block
        // leak into spoken answers verbatim.
        assertFalse(text.contains("2024-09-09"), line)
        assertFalse(text.contains("- Care Ghost"), "patients without records must be omitted")
    }

    @Test
    fun `given a population care question then totals include patients beyond detail cap`() {
        repos.patients.patients = (1L..12L).map { id -> testPatient(id, "Horse $id") }
        repos.vaccinations.entries =
            (1L..12L).map { id ->
                testVaccination(id, id, "Tetanus", administered = LocalDate(2025, 1, id.toInt()))
            }

        val summary = builder.build("Analyze my care data", today).orEmpty()

        assertTrue(summary.contains("CARE TOTALS: 12 vaccinations"), summary)
        assertTrue(summary.contains("across 12 patients with records"), summary)
        assertTrue(summary.contains("CARE DETAILS TRUNCATED: 2"), summary)
    }

    @Test
    fun `given a relative care period then older records do not enter the summary`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.vaccinations.entries =
            listOf(
                testVaccination(
                    id = 21,
                    patientId = 1,
                    vaccineName = "Tetanus",
                    administered = LocalDate(2024, 5, 1),
                ),
            )

        val summary = builder.build("What vaccination did Bella receive this month?", today).orEmpty()

        assertTrue(summary.contains("no care records found"), summary)
        assertFalse(summary.contains("1 vaccinations"), summary)
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
        assertTrue(text.contains("GESTATION TOTALS: 1 active pregnancy across 1 patient."), text)
        val line = text.lineSequence().first { it.startsWith("- Gestation") }
        // 2025-01-01 -> 2025-05-11 is exactly 130 days.
        assertTrue(line.contains("bred 1 Jan 2025"), line)
        assertTrue(line.contains("day 130,"), line)
        assertTrue(line.contains("status Active,"), line)
        assertTrue(line.contains("expected foaling 2025-12-07."), line)
        assertFalse(text.contains("Completed"), "resolved gestations must be excluded")
    }

    @Test
    fun `given multi-part gestation question then neutral capitalized words do not look like patient names`() {
        repos.patients.patients = listOf(testPatient(1, "Lua"), testPatient(2, "Estrela"))
        repos.gestations.entries =
            listOf(
                testGestation(41, 1, breedingDate = LocalDate(2025, 1, 1), expectedDueDate = LocalDate(2025, 12, 6)),
                testGestation(42, 2, breedingDate = LocalDate(2025, 2, 1), expectedDueDate = LocalDate(2025, 12, 31)),
            )

        val summary =
            builder.build(
                "Which mares are currently pregnant, how many days along are they, and when are they due?",
                today,
            )

        val text = summary.orEmpty()
        assertTrue(text.contains("Gestation Lua"), text)
        assertTrue(text.contains("Gestation Estrela"), text)
    }

    @Test
    fun `given current gestation facts then progress uses today instead of stored values`() {
        repos.patients.patients = listOf(testPatient(1, "Lua"))
        repos.gestations.entries =
            listOf(
                testGestation(
                    id = 41,
                    patientId = 1,
                    breedingDate = LocalDate(2025, 1, 1),
                    expectedDueDate = LocalDate(2000, 1, 1),
                ).copy(gestationDays = 1),
            )

        val facts = builder.gestationFacts("What is Lua's current gestation day and due date?", today)

        val currentFacts = requireNotNull(facts)
        assertEquals(1, currentFacts.size)
        assertEquals(130, currentFacts.single().progress.gestationDays)
        assertEquals(LocalDate(2025, 12, 7), currentFacts.single().progress.expectedDueDate)
        assertTrue(currentFacts.single().isActive)
        assertEquals(130, currentFacts.single().elapsedDays)
    }

    @Test
    fun `given breeding timing query then current facts include recorded date and elapsed days`() {
        repos.patients.patients = listOf(testPatient(1, "Lua"))
        repos.gestations.entries =
            listOf(
                testGestation(
                    id = 41,
                    patientId = 1,
                    breedingDate = LocalDate(2025, 1, 1),
                    expectedDueDate = LocalDate(2025, 12, 6),
                ),
            )

        val query = "How long ago was Lua bred?"
        val facts = builder.gestationFacts(query, today)

        assertTrue(AnalysisIntents.wantsBreedingTiming(query))
        assertEquals(LocalDate(2025, 1, 1), facts?.single()?.gestation?.breedingDate)
        assertEquals(130, facts?.single()?.elapsedDays)
    }

    @Test
    fun `given breeding card then breeding facts use its date and ignore pregnancy checks`() {
        repos.patients.patients = listOf(testPatient(1, "Lua"))
        repos.reproductions.entries =
            listOf(
                testReproductionEvent(
                    id = 61,
                    patientId = 1,
                    eventType = "Pregnancy Check",
                    date = LocalDate(2025, 4, 20),
                ),
                testReproductionEvent(
                    id = 60,
                    patientId = 1,
                    date = LocalDate(2025, 4, 1),
                ),
            )

        val query = "How long ago was Lua bred?"
        val facts = builder.breedingFacts(query, today)

        assertEquals(1, facts?.size)
        assertEquals(60L, facts?.single()?.event?.id)
        assertEquals(LocalDate(2025, 4, 1), facts?.single()?.event?.date)
        assertEquals(40, facts?.single()?.elapsedDays)
    }

    @Test
    fun `given named gestation question then summary excludes other patients`() {
        repos.patients.patients = listOf(testPatient(1, "Thunder"), testPatient(2, "Bella"))
        repos.gestations.entries =
            listOf(
                testGestation(41, 1, breedingDate = LocalDate(2025, 1, 1), expectedDueDate = LocalDate(2025, 12, 6)),
                testGestation(42, 2, breedingDate = LocalDate(2025, 2, 1), expectedDueDate = LocalDate(2025, 12, 31)),
            )

        val summary = builder.build("Is Thunder pregnant?", today).orEmpty()

        assertTrue(summary.contains("Gestation Thunder"), summary)
        assertFalse(summary.contains("Gestation Bella"), "named-patient summary leaked another horse: $summary")
    }

    @Test
    fun `given foaled gestation when built then resolved record is excluded`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))
        repos.gestations.entries =
            listOf(
                testGestation(
                    41,
                    1,
                    breedingDate = LocalDate(2025, 1, 1),
                    expectedDueDate = LocalDate(2025, 12, 6),
                    status = "Foaled",
                ),
            )

        val summary = builder.build("Which mares are pregnant?", today)

        assertTrue(summary.orEmpty().contains("no active pregnancies found"), summary)
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

        val lines = summary.orEmpty().lineSequence().count { it.startsWith("- OVERDUE ") && "TRUNCATED" !in it }
        assertEquals(12, lines)
    }

    @Test
    fun `given no overdue items when built then explicit empty overdue block is emitted`() {
        repos.patients.patients = listOf(testPatient(1, "Bella"))

        val summary = builder.build("Is any care overdue?", today)

        assertTrue(summary.orEmpty().contains("no overdue care found"), summary)
    }
}
