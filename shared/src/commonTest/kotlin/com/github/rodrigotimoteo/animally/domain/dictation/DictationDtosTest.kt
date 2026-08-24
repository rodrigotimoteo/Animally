package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import com.github.rodrigotimoteo.animally.domain.dictation.dto.SuggestedRecordDto
import com.github.rodrigotimoteo.animally.domain.dictation.dto.toSuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecordType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictationDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `when dto maps to domain then fields mirror`() {
        val dto =
            SuggestedRecordDto(
                recordType = "weight",
                patientName = "Trovão",
                date = "2026-08-20",
                weightKg = 512.5,
                notes = "após banho",
            )

        val record = dto.toSuggestedRecord()

        assertEquals(SuggestedRecordType.Weight, record?.recordType)
        assertEquals("Trovão", record?.patientName)
        assertEquals(512.5, record?.weightKg)
        assertEquals("após banho", record?.notes)
        assertNull(record?.ovaryStatus)
    }

    @Test
    fun `when record type differs by case then maps`() {
        val record = SuggestedRecordDto(recordType = "DEWORMING", drugName = "Ivermectina").toSuggestedRecord()

        assertEquals(SuggestedRecordType.Deworming, record?.recordType)
    }

    @Test
    fun `when unknown record type then mapper returns null`() {
        val record = SuggestedRecordDto(recordType = "dentistry").toSuggestedRecord()

        assertNull(record)
    }

    @Test
    fun `when session json decodes then round trip preserves records`() {
        val session =
            DictatedSessionDto(
                records =
                    listOf(
                        SuggestedRecordDto(
                            recordType = "ultrasound",
                            ovaryStatus = "pre-ovulatório",
                            follicleSizeMm = 35.0,
                        ),
                        SuggestedRecordDto(recordType = "weight", weightKg = 500.0),
                    ),
            )

        val decoded = json.decodeFromString<DictatedSessionDto>(json.encodeToString(session))

        assertEquals(session, decoded)
    }

    @Test
    fun `when session json has unknown keys then decode tolerates them`() {
        val payload =
            """{"records":[{"recordType":"weight","weightKg":500.0,"futureField":"x"}],"extra":1}"""

        val decoded = json.decodeFromString<DictatedSessionDto>(payload)

        assertEquals(1, decoded.records.size)
        assertEquals(500.0, decoded.records.single().weightKg)
    }
}
