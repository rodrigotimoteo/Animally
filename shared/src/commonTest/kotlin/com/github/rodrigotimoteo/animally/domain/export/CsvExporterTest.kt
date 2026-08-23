package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CsvExporterTest {
    private val exporter = CsvExporter()

    private val patient =
        Patient(
            id = 1L,
            name = "Thunder",
            species = "Equine",
            breed = "Arabian",
            dateOfBirth = LocalDate(2015, 5, 1),
            gender = "Gelding",
            ownerId = 7L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `escapes fields containing commas quotes and newlines`() {
        val consultation =
            Consultation(
                id = 1L,
                patientId = 1L,
                date = LocalDate(2024, 6, 1),
                subjective = "Colic, mild",
                objective = "Quote \"inside\"",
                assessment = "First line\nsecond line",
                plan = "Monitor",
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
            )

        val csv = exporter.exportPatientRecords(patient, ExportRecords(consultations = listOf(consultation)))

        assertTrue(csv.contains("\"Colic, mild\""))
        assertTrue(csv.contains("\"Quote \"\"inside\"\"\""))
        assertTrue(csv.contains("\"First line\nsecond line\""))
    }

    @Test
    fun `writes human-readable header rows with record type first`() {
        val csv = exporter.exportPatientRecords(patient, ExportRecords())

        assertTrue(csv.contains("Record Type,ID,Name,Species,Breed,Date of Birth,Gender,Microchip,UELN,Registration Number,Stable Location,Notes,Owner,Active"))
        assertTrue(csv.contains("Record Type,ID,Patient,Date,Subjective,Objective,Assessment,Plan,Veterinarian,Next Visit Date"))
    }

    @Test
    fun `empty record lists yield header rows only`() {
        val csv = exporter.exportPatientRecords(patient, ExportRecords())

        val dataRows =
            csv.lines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Record Type,") }

        assertEquals(1, dataRows.size)
        assertTrue(dataRows.single().startsWith("Patient,1,Thunder"))
        assertEquals(18, csv.lines().count { it.startsWith("Record Type,") })
    }

    @Test
    fun `emits a section header for every entity type`() {
        val csv = exporter.exportPatientRecords(patient, ExportRecords())

        val sectionTitles =
            listOf(
                "Patient",
                "Anamnese",
                "Weight",
                "Consultation",
                "Vaccination",
                "Deworming",
                "Dentistry",
                "Lameness",
                "Surgery",
                "Medication",
                "LabResult",
                "Imaging",
                "FarrierVisit",
                "ReproductionEvent",
                "Ultrasound",
                "Gestation",
                "ReproMedication",
                "ControlledSubstance",
            )

        sectionTitles.forEach { sectionTitle ->
            assertTrue(csv.contains("# $sectionTitle"), "Missing section: $sectionTitle")
        }
    }

    @Test
    fun `formats patient demographics and consultation rows after headers`() {
        val consultation =
            Consultation(
                id = 42L,
                patientId = 1L,
                date = LocalDate(2024, 6, 1),
                subjective = "Colic, mild",
                objective = "Tension in flank",
                assessment = "Suspected tendonitis",
                plan = "Monitor",
                vetName = "Dr. House",
                nextVisitDate = LocalDate(2024, 7, 1),
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
            )

        val csv = exporter.exportPatientRecords(patient, ExportRecords(consultations = listOf(consultation)))
        val lines = csv.lines().filter { it.isNotBlank() }

        assertEquals("# Patient", lines.first())
        assertTrue(lines.contains("Consultation,42,1,2024-06-01,\"Colic, mild\",Tension in flank,Suspected tendonitis,Monitor,Dr. House,2024-07-01"))
        assertTrue(lines.any { it.startsWith("Patient,1,Thunder") && it.contains("Equine") && it.contains("Arabian") })
        assertTrue(lines.any { it.startsWith("Consultation,42,1,2024-06-01") })
    }

    @Test
    fun `maps internal field names to display headers`() {
        listOf(
            "VetName" to "Veterinarian",
            "PatientId" to "Patient",
            "NextDueDate" to "Next Due Date",
            "UELN" to "UELN",
            "WeightKg" to "Weight (kg)",
            "GradeAAEP" to "AAEP Grade",
        ).forEach { (internal, display) ->
            assertEquals(display, CsvFormatter.displayHeader(internal), "mapping for $internal")
        }
    }
}
