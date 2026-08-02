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
    fun `writes a header row for every section`() {
        val csv = exporter.exportPatientRecords(patient, ExportRecords())

        assertTrue(csv.contains("Id,Name,Species,Breed,DateOfBirth,Gender,MicrochipId,UELN,RegistrationNumber,StableLocation,Notes,OwnerId,Active"))
        assertTrue(csv.contains("Id,PatientId,Date,Subjective,Objective,Assessment,Plan,VetName,NextVisitDate"))
    }

    @Test
    fun `empty record lists yield header rows only`() {
        val csv = exporter.exportPatientRecords(patient, ExportRecords())

        val dataRows =
            csv.lines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Id,") }

        assertEquals(1, dataRows.size)
        assertTrue(dataRows.single().startsWith("1,Thunder"))
        assertEquals(18, csv.lines().count { it.startsWith("Id,") })
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
}
