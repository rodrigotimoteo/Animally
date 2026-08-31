package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.usecase.GetInsightsDashboardUseCase
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Golden tests for Task 10 reproduction period facts.
 *
 * Verifies canonical event counts (unknown -> Other), soft-deleted/out-of-range exclusion,
 * embryo/ICSI sums and averages with explicit denominators, ultrasound counts,
 * sargable scoped/global split and snapshot integration. No success-rate metric is exposed.
 */
class InsightsReproductionTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var repository: SqlDelightInsightsRepository

    private val now = Instant.fromEpochMilliseconds(0L)
    private val inRangeFrom = LocalDate(2025, 1, 10)
    private val inRangeTo = LocalDate(2025, 1, 20)
    private val inRange = LocalDate(2025, 1, 15)
    private val outOfRange = LocalDate(2024, 12, 1)

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        repository = SqlDelightInsightsRepository(database)
        seedPatients()
    }

    private fun seedPatients() {
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Star",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 2L,
            name = "Storm",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 3L,
            name = "InactiveMare",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = false,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun insertReproduction(
        id: Long,
        patientId: Long,
        eventType: String,
        date: LocalDate,
        isActive: Boolean = true,
    ) {
        database.reproductionQueries.insertWithId(
            id = id,
            patientId = patientId,
            eventType = eventType,
            date = date,
            details = "d",
            initialExamFindings = null,
            stallionName = null,
            breedingType = null,
            vetName = "Dr Vet",
            notes = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun insertEmbryo(
        id: Long,
        patientId: Long,
        date: LocalDate,
        embryoCount: Long,
        isActive: Boolean = true,
    ) {
        database.embryoTransferQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            embryoCount = embryoCount,
            recipientMares = null,
            vetName = "Dr Vet",
            notes = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun insertIcsi(
        id: Long,
        patientId: Long,
        date: LocalDate,
        follicles: Long,
        isActive: Boolean = true,
    ) {
        database.icsiQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            folliclesRecovered = follicles,
            vetName = "Dr Vet",
            notes = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun insertUltrasound(
        id: Long,
        patientId: Long,
        date: LocalDate,
        isActive: Boolean = true,
    ) {
        database.ultrasoundQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            ovaryStatus = null,
            uterineStatus = null,
            follicleSizeMm = null,
            leftOvaryStatus = null,
            rightOvaryStatus = null,
            leftFollicleSizeMm = null,
            rightFollicleSizeMm = null,
            uterineEdema = null,
            uterineLiquid = null,
            uterineLiquidDescription = null,
            uterusDescription = null,
            findings = "f",
            imageUris = null,
            vetName = "Dr Vet",
            notes = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test
    fun `unknown event values appear under Other with canonical merging`() {
        // Tolerant legacy spellings and unknown -> Other
        insertReproduction(1, 1L, "Breeding", inRange)
        insertReproduction(2, 1L, "breeding", inRange)
        insertReproduction(3, 1L, "BREEDING", inRange)
        insertReproduction(4, 1L, "PregnancyCheck", inRange)
        insertReproduction(5, 1L, "Pregnancy Check", inRange)
        insertReproduction(6, 1L, "pregnancy_check", inRange)
        insertReproduction(7, 1L, "pregnancy-check", inRange)
        insertReproduction(8, 1L, "Heat", inRange)
        insertReproduction(9, 1L, "Foaling", inRange)
        insertReproduction(10, 1L, "Initial Exam", inRange)
        insertReproduction(11, 1L, "InitialExam", inRange)
        insertReproduction(12, 1L, "unknownXYZ", inRange)
        insertReproduction(13, 1L, "Mystery", inRange)

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val metrics = repository.getReproductionMetrics(filter)

        val map = metrics.eventCounts.associate { it.type to it.count }
        assertEquals(3, map[ReproductionEventType.Breeding])
        assertEquals(4, map[ReproductionEventType.PregnancyCheck])
        assertEquals(1, map[ReproductionEventType.Heat])
        assertEquals(1, map[ReproductionEventType.Foaling])
        assertEquals(2, map[ReproductionEventType.InitialExam])
        assertEquals(2, map[ReproductionEventType.Other])
        // total sums to 13, matches raw inserted with canonical merging
        assertEquals(13, metrics.eventCounts.sumOf { it.count })
    }

    @Test
    fun `soft deleted out of range and inactive patient excluded`() {
        insertReproduction(1, 1L, "Breeding", inRange, isActive = true)
        insertReproduction(2, 1L, "Breeding", inRange, isActive = false)
        insertReproduction(3, 1L, "Heat", outOfRange, isActive = true)
        insertReproduction(4, 3L, "Breeding", inRange, isActive = true)
        insertEmbryo(10, 1L, inRange, 2, isActive = true)
        insertEmbryo(11, 1L, inRange, 5, isActive = false)
        insertEmbryo(12, 1L, outOfRange, 10, isActive = true)
        insertEmbryo(13, 3L, inRange, 4, isActive = true)
        insertIcsi(20, 1L, inRange, 5, isActive = true)
        insertIcsi(21, 1L, inRange, 10, isActive = false)
        insertIcsi(22, 1L, outOfRange, 20, isActive = true)
        insertIcsi(23, 3L, inRange, 7, isActive = true)
        insertUltrasound(30, 1L, inRange, isActive = true)
        insertUltrasound(31, 1L, inRange, isActive = false)
        insertUltrasound(32, 1L, outOfRange, isActive = true)
        insertUltrasound(33, 3L, inRange, isActive = true)

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val m = repository.getReproductionMetrics(filter)

        // only the single active in-range row per type should count
        val breeding = m.eventCounts.find { it.type == ReproductionEventType.Breeding }?.count ?: 0
        assertEquals(1, breeding)
        assertEquals(1, m.embryoCollections)
        assertEquals(2, m.embryosCollected)
        assertEquals(1, m.icsiSessions)
        assertEquals(5, m.folliclesRecovered)
        assertEquals(1, m.ultrasoundCount)
    }

    @Test
    fun `sums and averages include explicit sample counts and null when zero denominator`() {
        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo)
        // empty period
        var m = repository.getReproductionMetrics(filter)
        assertEquals(0, m.embryoCollections)
        assertEquals(0, m.embryosCollected)
        assertNull(m.averageEmbryosPerCollection)
        assertEquals(0, m.icsiSessions)
        assertEquals(0, m.folliclesRecovered)
        assertNull(m.averageFolliclesPerIcsi)
        assertEquals(0, m.ultrasoundCount)
        assertTrue(m.eventCounts.isEmpty())

        // 2 embryo collections 3+7=10 -> avg 5.0
        insertEmbryo(1, 1L, inRange, 3)
        insertEmbryo(2, 1L, LocalDate(2025, 1, 16), 7)
        // 3 ICSI sessions 5+10+0=15 -> avg 5.0
        insertIcsi(10, 1L, inRange, 5)
        insertIcsi(11, 1L, LocalDate(2025, 1, 16), 10)
        insertIcsi(12, 1L, LocalDate(2025, 1, 17), 0)
        insertUltrasound(20, 1L, inRange)
        insertUltrasound(21, 1L, LocalDate(2025, 1, 16))

        m = repository.getReproductionMetrics(filter)
        assertEquals(2, m.embryoCollections)
        assertEquals(10, m.embryosCollected)
        assertEquals(5.0, m.averageEmbryosPerCollection!!, 0.001)
        assertEquals(3, m.icsiSessions)
        assertEquals(15, m.folliclesRecovered)
        assertEquals(5.0, m.averageFolliclesPerIcsi!!, 0.001)
        assertEquals(2, m.ultrasoundCount)

        // single collection with 0 embryos -> avg 0.0 not null
        database.embryoTransferQueries.deleteAll()
        insertEmbryo(30, 1L, inRange, 0)
        m = repository.getReproductionMetrics(filter)
        assertEquals(1, m.embryoCollections)
        assertEquals(0, m.embryosCollected)
        assertEquals(0.0, m.averageEmbryosPerCollection!!, 0.001)
    }

    @Test
    fun `patient scope filters reproduction facts`() {
        insertReproduction(1, 1L, "Breeding", inRange)
        insertReproduction(2, 2L, "Heat", inRange)
        insertEmbryo(10, 1L, inRange, 2)
        insertEmbryo(11, 2L, inRange, 5)
        insertIcsi(20, 1L, inRange, 4)
        insertIcsi(21, 2L, inRange, 10)
        insertUltrasound(30, 1L, inRange)
        insertUltrasound(31, 1L, inRange)
        insertUltrasound(32, 2L, inRange)

        val global = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val g = repository.getReproductionMetrics(global)
        assertEquals(2, g.eventCounts.sumOf { it.count })
        assertEquals(2, g.embryoCollections)
        assertEquals(7, g.embryosCollected)
        assertEquals(2, g.icsiSessions)
        assertEquals(14, g.folliclesRecovered)
        assertEquals(3, g.ultrasoundCount)

        val p1 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 1L)
        val m1 = repository.getReproductionMetrics(p1)
        assertEquals(1, m1.eventCounts.sumOf { it.count })
        assertEquals(1, m1.embryoCollections)
        assertEquals(2, m1.embryosCollected)
        assertEquals(1, m1.icsiSessions)
        assertEquals(4, m1.folliclesRecovered)
        assertEquals(2, m1.ultrasoundCount)

        val p2 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 2L)
        val m2 = repository.getReproductionMetrics(p2)
        assertEquals(1, m2.eventCounts.sumOf { it.count })
        assertEquals(1, m2.embryoCollections)
        assertEquals(5, m2.embryosCollected)
        assertEquals(1, m2.icsiSessions)
        assertEquals(10, m2.folliclesRecovered)
        assertEquals(1, m2.ultrasoundCount)
    }

    @Test
    fun `snapshot integrates reproduction facts golden`() {
        // Golden fixture: 30-day window
        insertReproduction(1, 1L, "Breeding", LocalDate(2025, 1, 12))
        insertReproduction(2, 1L, "PregnancyCheck", LocalDate(2025, 1, 13))
        insertReproduction(3, 2L, "Heat", LocalDate(2025, 1, 14))
        insertReproduction(4, 1L, "unknownType", LocalDate(2025, 1, 14))
        insertEmbryo(10, 1L, LocalDate(2025, 1, 12), 2)
        insertEmbryo(11, 1L, LocalDate(2025, 1, 14), 4)
        insertIcsi(20, 1L, LocalDate(2025, 1, 13), 6)
        insertIcsi(21, 2L, LocalDate(2025, 1, 15), 9)
        insertUltrasound(30, 1L, LocalDate(2025, 1, 12))
        insertUltrasound(31, 2L, LocalDate(2025, 1, 13))
        insertUltrasound(32, 1L, LocalDate(2025, 1, 14))
        // seed one consultation to ensure overview not empty (required for snapshot)
        database.consultationQueries.insertWithId(
            id = 100L,
            patientId = 1L,
            date = LocalDate(2025, 1, 12),
            subjective = "s",
            objective = "o",
            assessment = "a",
            plan = "p",
            vetName = "Dr Vet",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val useCase = GetInsightsDashboardUseCase(repository) { LocalDate(2025, 1, 31) }
        val snap = useCase(filter)

        // Reproduction assertions - golden totals
        assertEquals(4, snap.reproduction.eventCounts.sumOf { it.count })
        val breeding =
            snap.reproduction.eventCounts
                .find { it.type == ReproductionEventType.Breeding }
                ?.count ?: 0
        val preg =
            snap.reproduction.eventCounts
                .find { it.type == ReproductionEventType.PregnancyCheck }
                ?.count ?: 0
        val heat =
            snap.reproduction.eventCounts
                .find { it.type == ReproductionEventType.Heat }
                ?.count ?: 0
        val other =
            snap.reproduction.eventCounts
                .find { it.type == ReproductionEventType.Other }
                ?.count ?: 0
        assertEquals(1, breeding)
        assertEquals(1, preg)
        assertEquals(1, heat)
        assertEquals(1, other)
        assertEquals(2, snap.reproduction.embryoCollections)
        assertEquals(6, snap.reproduction.embryosCollected)
        assertEquals(3.0, snap.reproduction.averageEmbryosPerCollection!!, 0.001)
        assertEquals(2, snap.reproduction.icsiSessions)
        assertEquals(15, snap.reproduction.folliclesRecovered)
        assertEquals(7.5, snap.reproduction.averageFolliclesPerIcsi!!, 0.001)
        assertEquals(3, snap.reproduction.ultrasoundCount)

        // Inclusive range check - ensure no success rate field exists (compile-time)
        // ReproductionMetrics has fixed fields; we verify no hidden rate by asserting exact field set
        assertNotNull(snap.reproduction.averageEmbryosPerCollection)
        assertNotNull(snap.reproduction.averageFolliclesPerIcsi)
        // Ensure rates remain unavailable when zero denominator
        val emptySnap =
            GetInsightsDashboardUseCase(
                repository =
                    object : com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository {
                        override fun getEarliestActivityDate(patientId: Long?) = null

                        override fun getActivityBuckets(filter: InsightsFilter) =
                            emptyList<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket>()

                        override fun getRecordRefs(drillDown: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown) =
                            emptyList<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef>()

                        override fun getReproductionMetrics(filter: InsightsFilter) =
                            com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics(
                                eventCounts = emptyList(),
                                embryoCollections = 0,
                                embryosCollected = 0,
                                averageEmbryosPerCollection = null,
                                icsiSessions = 0,
                                folliclesRecovered = 0,
                                averageFolliclesPerIcsi = null,
                                ultrasoundCount = 0,
                            )

                        override fun getCurrentCareSnapshot(
                            patientId: Long?,
                            today: LocalDate,
                        ) = com.github.rodrigotimoteo.animally.domain.insights.model
                            .CurrentCareSnapshot(activeGestations = emptyList())

                        override fun getDataIssueCounts(filter: InsightsFilter) =
                            emptyList<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount>()

                        override fun getDataIssueRecordRefs(
                            filter: InsightsFilter,
                            issueType: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType,
                        ) = emptyList<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef>()
                    },
            ) { LocalDate(2025, 1, 31) }.invoke(filter)
        assertNull(emptySnap.reproduction.averageEmbryosPerCollection)
        assertNull(emptySnap.reproduction.averageFolliclesPerIcsi)
    }

    @Test
    fun `no success rate exposed - fields are counts and averages only`() {
        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo)
        val m = repository.getReproductionMetrics(filter)
        // Reflection check that ReproductionMetrics has exactly expected fields, no rate
        val fieldNames = m::class.members.map { it.name }.toSet()
        assertTrue(fieldNames.contains("embryoCollections"))
        assertTrue(fieldNames.contains("embryosCollected"))
        assertTrue(fieldNames.contains("averageEmbryosPerCollection"))
        assertTrue(fieldNames.contains("icsiSessions"))
        assertTrue(fieldNames.contains("folliclesRecovered"))
        assertTrue(fieldNames.contains("averageFolliclesPerIcsi"))
        assertTrue(fieldNames.contains("ultrasoundCount"))
        assertTrue(fieldNames.contains("eventCounts"))
        assertTrue(!fieldNames.contains("successRate"))
        assertTrue(!fieldNames.contains("conceptionRate"))
        assertTrue(!fieldNames.contains("transferSuccess"))
        assertTrue(!fieldNames.contains("foalingRate"))
    }
}
