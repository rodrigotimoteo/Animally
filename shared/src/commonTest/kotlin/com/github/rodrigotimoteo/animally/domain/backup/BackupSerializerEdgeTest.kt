package com.github.rodrigotimoteo.animally.domain.backup

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Serializer edge cases: tolerance for unknown keys and for legacy payloads
 * written before newer collections existed.
 */
class BackupSerializerEdgeTest {
    @Test
    fun `decode tolerates unknown keys at payload and row level`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-01-01T00:00:00Z",
              "futureTopLevelField": 42,
              "patients": [
                {
                  "id": 1,
                  "name": "Charlie",
                  "species": "Equine",
                  "breed": null,
                  "dateOfBirth": null,
                  "gender": null,
                  "microchipId": null,
                  "ueln": null,
                  "registrationNumber": null,
                  "stableLocation": null,
                  "photoUri": null,
                  "notes": null,
                  "ownerId": null,
                  "isActive": true,
                  "createdAt": 1000,
                  "updatedAt": 2000,
                  "cogginsTestDate": null,
                  "cogginsResult": null,
                  "cogginsExpiryDate": null,
                  "futureRowField": "ignored"
                }
              ],
              "owners": [],
              "anamnese": [],
              "consultations": [],
              "vaccinations": [],
              "weights": [],
              "dewormings": [],
              "dentistry": [],
              "lameness": [],
              "surgeries": [],
              "medications": [],
              "labResults": [],
              "imaging": [],
              "farrierVisits": [],
              "reproductionEvents": [],
              "ultrasounds": [],
              "gestations": [],
              "reproMedications": [],
              "substances": []
            }
            """.trimIndent()

        val payload = BackupSerializer.decode(json)

        assertEquals(1, payload.patients.size)
        assertEquals("Charlie", payload.patients.single().name)
    }

    @Test
    fun `decode accepts legacy payload without post-v1 collections`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-01-01T00:00:00Z",
              "patients": [],
              "owners": [],
              "anamnese": [],
              "consultations": [],
              "vaccinations": [],
              "weights": [],
              "dewormings": [],
              "dentistry": [],
              "lameness": [],
              "surgeries": [],
              "medications": [],
              "labResults": [],
              "imaging": [],
              "farrierVisits": [],
              "reproductionEvents": [],
              "ultrasounds": [],
              "gestations": [],
              "reproMedications": [],
              "substances": []
            }
            """.trimIndent()

        val payload = BackupSerializer.decode(json)

        assertTrue(payload.follicles.isEmpty())
        assertTrue(payload.embryoTransfers.isEmpty())
        assertTrue(payload.icsi.isEmpty())
        assertTrue(payload.customReminders.isEmpty())
    }

    @Test
    fun `encode then decode preserves custom reminders`() {
        val payload =
            validMinimalPayload()
                .copy(
                    customReminders =
                        listOf(
                            CustomReminderDto(
                                id = 110L,
                                patientId = 1L,
                                title = "Annual check",
                                dueDate = LocalDate(2026, 10, 20),
                                linkedRecordType = "Vaccination",
                                linkedRecordId = 90L,
                                notes = null,
                                isActive = true,
                                createdAt = Instant.fromEpochMilliseconds(11000L),
                                updatedAt = Instant.fromEpochMilliseconds(11100L),
                            ),
                        ),
                )

        val decoded = BackupSerializer.decode(BackupSerializer.encode(payload))

        assertEquals(payload, decoded)
    }

    private fun validMinimalPayload(): BackupPayload =
        BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = "2026-08-02T10:00:00Z",
            patients = emptyList(),
            owners = emptyList(),
            anamnese = emptyList(),
            consultations = emptyList(),
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
}
