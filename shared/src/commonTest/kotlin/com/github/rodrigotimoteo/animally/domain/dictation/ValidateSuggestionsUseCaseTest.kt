package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.dto.SuggestedRecordDto
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecordType
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ValidateSuggestionsUseCaseTest {
    private val sut = ValidateSuggestionsUseCase()
    private val today = LocalDate(2026, 8, 24)

    private fun dto(
        recordType: String = "ultrasound",
        date: String? = "2026-08-20",
        weightKg: Double? = null,
        ovaryStatus: String? = null,
        uterineStatus: String? = null,
        follicleSizeMm: Double? = null,
        drugName: String? = null,
        notes: String? = null,
    ) = SuggestedRecordDto(
        recordType = recordType,
        patientName = "Trovao",
        date = date,
        weightKg = weightKg,
        ovaryStatus = ovaryStatus,
        uterineStatus = uterineStatus,
        follicleSizeMm = follicleSizeMm,
        drugName = drugName,
        notes = notes,
    )

    @Test
    fun `when unknown record type then suggestion is excluded`() {
        val result = sut(listOf(dto(recordType = "dentistry")), today)

        assertEquals(0, result.size)
    }

    @Test
    fun `when only date present then dropped`() {
        val result = sut(listOf(dto()), today)

        assertIs<SuggestedValidationState.Dropped>(result.single().validation)
    }

    @Test
    fun `when happy path ultrasound then ok`() {
        val result =
            sut(
                listOf(dto(ovaryStatus = "pre-ovulatório", follicleSizeMm = 35.0)),
                today,
            )

        val record = result.single()
        assertEquals(SuggestedRecordType.Ultrasound, record.recordType)
        assertEquals(LocalDate(2026, 8, 20), record.date)
        assertEquals(SuggestedValidationState.Ok, record.validation)
    }

    @Test
    fun `when date unparseable then defaults to today silently`() {
        val result = sut(listOf(dto(date = "ontem", weightKg = 500.0)), today)

        val record = result.single()
        assertEquals(today, record.date)
        assertEquals(SuggestedValidationState.Ok, record.validation)
    }

    @Test
    fun `when date absent then defaults to today silently`() {
        val result = sut(listOf(dto(date = null, weightKg = 500.0)), today)

        val record = result.single()
        assertEquals(today, record.date)
        assertEquals(SuggestedValidationState.Ok, record.validation)
    }

    @Test
    fun `when date older than 365 days then flagged and kept`() {
        val result = sut(listOf(dto(date = "2025-08-23", weightKg = 500.0)), today)

        val record = result.single()
        assertEquals(LocalDate(2025, 8, 23), record.date)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
        assertEquals(listOf("date_out_of_range"), record.validation.reasons)
    }

    @Test
    fun `when date in future then flagged and kept`() {
        val result = sut(listOf(dto(date = "2026-09-01", weightKg = 500.0)), today)

        val record = result.single()
        assertEquals(LocalDate(2026, 9, 1), record.date)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
    }

    @Test
    fun `when weight not positive then nulled and flagged`() {
        val result = sut(listOf(dto(recordType = "weight", weightKg = -1.0)), today)

        val record = result.single()
        assertNull(record.weightKg)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
        assertEquals(listOf("weight_implausible"), record.validation.reasons)
    }

    @Test
    fun `when weight above 3000 kg then nulled and flagged`() {
        val result = sut(listOf(dto(recordType = "weight", weightKg = 3500.0)), today)

        val record = result.single()
        assertNull(record.weightKg)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
    }

    @Test
    fun `when weight between 1500 and 3000 kg then kept and flagged`() {
        val result = sut(listOf(dto(recordType = "weight", weightKg = 2000.0)), today)

        val record = result.single()
        assertEquals(2000.0, record.weightKg)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
        assertEquals(listOf("weight_high"), record.validation.reasons)
    }

    @Test
    fun `when follicle size not positive then kept and flagged`() {
        val result = sut(listOf(dto(follicleSizeMm = 0.0)), today)

        val record = result.single()
        assertEquals(0.0, record.follicleSizeMm)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
        assertEquals(listOf("follicle_size_implausible"), record.validation.reasons)
    }

    @Test
    fun `when follicle size above 100 mm then kept and flagged`() {
        val result = sut(listOf(dto(follicleSizeMm = 120.0)), today)

        val record = result.single()
        assertEquals(120.0, record.follicleSizeMm)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
    }

    @Test
    fun `when drug name longer than 100 chars then truncated and flagged`() {
        val longName = "I".repeat(120)
        val result = sut(listOf(dto(recordType = "deworming", drugName = longName)), today)

        val record = result.single()
        assertEquals(100, record.drugName?.length)
        assertIs<SuggestedValidationState.Flagged>(record.validation)
        assertEquals(listOf("drug_name_truncated"), record.validation.reasons)
    }

    @Test
    fun `when multiple issues then reasons accumulate`() {
        val result =
            sut(
                listOf(
                    dto(
                        date = "2024-01-01",
                        weightKg = 4000.0,
                        follicleSizeMm = 250.0,
                    ),
                ),
                today,
            )

        val validation = assertIs<SuggestedValidationState.Flagged>(result.single().validation)
        assertEquals(
            listOf("date_out_of_range", "weight_implausible", "follicle_size_implausible"),
            validation.reasons,
        )
    }
}
