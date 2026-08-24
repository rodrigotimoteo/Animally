package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.SaveDewormingUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecord
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedRecordType
import com.github.rodrigotimoteo.animally.domain.dictation.model.SuggestedValidationState
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.SaveUltrasoundUseCase
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.SaveWeightUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class InsertSuggestionsUseCaseTest {
    private val weightRepository = FakeWeightRepository()
    private val dewormingRepository = FakeDewormingRepository()
    private val ultrasoundRepository = FakeUltrasoundRepository()

    private val sut =
        InsertSuggestionsUseCase(
            saveUltrasoundUseCase = SaveUltrasoundUseCase(ultrasoundRepository, FakeSearchRepository()),
            saveWeightUseCase = SaveWeightUseCase(weightRepository, FakeSearchRepository()),
            saveDewormingUseCase = SaveDewormingUseCase(dewormingRepository, FakeSearchRepository()),
        )

    private fun record(
        validation: SuggestedValidationState,
        recordType: SuggestedRecordType = SuggestedRecordType.Weight,
    ) = SuggestedRecord(
        recordType = recordType,
        date = LocalDate(2026, 8, 20),
        weightKg = if (recordType == SuggestedRecordType.Weight) 500.0 else null,
        drugName = if (recordType == SuggestedRecordType.Deworming) "Ivermectina" else null,
        ovaryStatus = if (recordType == SuggestedRecordType.Ultrasound) "normal" else null,
        validation = validation,
    )

    @Test
    fun `when dropped record then rejected and nothing saved`() {
        val result =
            sut(listOf(SuggestedInsertion(record(SuggestedValidationState.Dropped), patientId = 1L)))

        val failed = assertIs<InsertionResult.Failed>(result.single())
        assertEquals("dropped record cannot be inserted", failed.message)
        assertTrue(weightRepository.inserted.isEmpty())
    }

    @Test
    fun `when flagged record without acknowledgement then rejected`() {
        val result =
            sut(
                listOf(
                    SuggestedInsertion(
                        record(SuggestedValidationState.Flagged(listOf("weight_high"))),
                        patientId = 1L,
                    ),
                ),
            )

        val failed = assertIs<InsertionResult.Failed>(result.single())
        assertEquals("flagged record not accepted", failed.message)
        assertTrue(weightRepository.inserted.isEmpty())
    }

    @Test
    fun `when flagged record with acknowledgement then inserts`() {
        val result =
            sut(
                listOf(
                    SuggestedInsertion(
                        record(SuggestedValidationState.Flagged(listOf("weight_high"))),
                        patientId = 1L,
                        acknowledgedFlags = true,
                    ),
                ),
            )

        assertIs<InsertionResult.Inserted>(result.single())
        assertEquals(1, weightRepository.inserted.size)
    }

    @Test
    fun `when ok record then inserts`() {
        val result = sut(listOf(SuggestedInsertion(record(SuggestedValidationState.Ok), patientId = 7L)))

        val inserted = assertIs<InsertionResult.Inserted>(result.single())
        assertEquals(SuggestedRecordType.Weight, inserted.recordType)
        assertEquals(7L, weightRepository.inserted.single().patientId)
    }

    @Test
    fun `when mixed batch then each outcome independent and in order`() {
        val result =
            sut(
                listOf(
                    SuggestedInsertion(record(SuggestedValidationState.Ok), patientId = 1L),
                    SuggestedInsertion(record(SuggestedValidationState.Dropped), patientId = 1L),
                    SuggestedInsertion(
                        record(SuggestedValidationState.Flagged(listOf("date_unparseable"))),
                        patientId = 1L,
                        acknowledgedFlags = true,
                    ),
                ),
            )

        assertIs<InsertionResult.Inserted>(result[0])
        assertIs<InsertionResult.Failed>(result[1])
        assertIs<InsertionResult.Inserted>(result[2])
        assertEquals(2, weightRepository.inserted.size)
    }
}

private class FakeWeightRepository : IWeightRepository {
    val inserted = mutableListOf<Weight>()

    override fun getByPatient(patientId: Long): List<Weight> = emptyList()

    override fun getById(id: Long): Weight? = null

    override fun insert(weight: Weight): Long {
        inserted += weight
        return inserted.size.toLong()
    }

    override fun update(weight: Weight): Long = weight.id

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = id
}

private class FakeDewormingRepository : IDewormingRepository {
    override fun getByPatient(patientId: Long): List<Deworming> = emptyList()

    override fun getById(id: Long): Deworming? = null

    override fun insert(deworming: Deworming): Long = 1L

    override fun update(deworming: Deworming): Long = deworming.id

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = id
}

private class FakeUltrasoundRepository : IUltrasoundRepository {
    override fun getByPatient(patientId: Long): List<Ultrasound> = emptyList()

    override fun getById(id: Long): Ultrasound? = null

    override fun insert(ultrasound: Ultrasound): Long = 1L

    override fun update(ultrasound: Ultrasound): Long = ultrasound.id

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = id
}

private class FakeSearchRepository : ISearchRepository {
    override fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> = emptyList()

    override fun searchSnippets(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> = emptyList()

    override fun indexRecord(
        recordType: String,
        patientId: Long,
        recordId: Long,
        date: LocalDate?,
        searchableText: String,
    ) = Unit

    override fun deleteRecord(
        recordType: String,
        recordId: Long,
    ) = Unit

    override fun rebuild() = Unit

    override fun reindexOwners() = Unit

    override fun reindexPatients() = Unit

    override fun reindexRecords() = Unit

    override fun reindexIfNeeded(indexVersion: String) = Unit
}
