package com.github.rodrigotimoteo.animally.domain.backup

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class BackupSerializerTest {
    private val samplePayload: BackupPayload =
        BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = "2026-08-02T10:00:00Z",
            patients =
                listOf(
                    PatientDto(
                        id = 1L,
                        name = "Charlie",
                        species = "Equine",
                        breed = "Hanoverian",
                        dateOfBirth = LocalDate(2018, 3, 1),
                        gender = "Mare",
                        microchipId = "981000000000000",
                        ueln = null,
                        registrationNumber = null,
                        stableLocation = "Stable A",
                        photoUri = null,
                        notes = "Coggins current",
                        ownerId = 1L,
                        isActive = true,
                        createdAt = Instant.fromEpochMilliseconds(1000L),
                        updatedAt = Instant.fromEpochMilliseconds(2000L),
                        cogginsTestDate = LocalDate(2025, 1, 10),
                        cogginsResult = "Negative",
                        cogginsExpiryDate = LocalDate(2025, 7, 10),
                    ),
                ),
            owners =
                listOf(
                    OwnerDto(
                        id = 1L,
                        name = "Jane Doe",
                        email = "jane@example.com",
                        phone = null,
                        address = null,
                        isActive = true,
                        createdAt = Instant.fromEpochMilliseconds(1000L),
                        updatedAt = Instant.fromEpochMilliseconds(1000L),
                    ),
                ),
            anamnese = emptyList(),
            consultations =
                listOf(
                    ConsultationDto(
                        id = 5L,
                        patientId = 1L,
                        date = LocalDate(2026, 6, 15),
                        subjective = "Mild lameness",
                        objective = "Swelling noted",
                        assessment = "Suspected tendonitis",
                        plan = "Rest and recheck",
                        vetName = "Dr. Vet",
                        nextVisitDate = LocalDate(2026, 7, 1),
                        isActive = false,
                        createdAt = Instant.fromEpochMilliseconds(3000L),
                        updatedAt = Instant.fromEpochMilliseconds(4000L),
                    ),
                ),
            vaccinations = emptyList(),
            weights = emptyList(),
            dewormings = emptyList(),
            dentistry = emptyList(),
            lameness = emptyList(),
            surgeries = emptyList(),
            medications = emptyList(),
            labResults = emptyList(),
            imaging = emptyList(),
            farrierVisits = emptyList(),
            reproductionEvents = emptyList(),
            ultrasounds = emptyList(),
            gestations = emptyList(),
            reproMedications = emptyList(),
            substances = emptyList(),
        )

    @Test
    fun `encode then decode round-trips every field`() {
        val json = BackupSerializer.encode(samplePayload)

        val decoded = BackupSerializer.decode(json)

        assertEquals(samplePayload, decoded)
    }

    @Test
    fun `encode produces pretty json with date and epoch formats`() {
        val json = BackupSerializer.encode(samplePayload)

        assertEquals(true, json.contains("2026-08-02T10:00:00Z"))
        assertEquals(true, json.contains("\"cogginsTestDate\": \"2025-01-10\""))
        assertEquals(true, json.contains("\"createdAt\": 1000"))
        assertEquals(true, json.contains("\"nextVisitDate\": \"2026-07-01\""))
    }

    @Test
    fun `decode rejects payload with different schema version`() {
        val json = BackupSerializer.encode(samplePayload.copy(schemaVersion = 99))

        assertFailsWith<IllegalStateException> {
            BackupSerializer.decode(json)
        }
    }
}
