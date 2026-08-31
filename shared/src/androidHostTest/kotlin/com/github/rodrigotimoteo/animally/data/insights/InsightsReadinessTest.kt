package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.usecase.GetInsightsDashboardUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Golden tests for Task 13 research-readiness drill-down.
 *
 * Verifies 5 explicit rules: unknown reproduction category, missing vet name,
 * unlinked owner, free-text embryo recipient, incomplete ultrasound.
 * Each rule has definition surfacing as "missing for analysis" not clinically wrong,
 * and drills to affected rows only. Checks inclusive range, isActive, Patient.isActive,
 * whitespace handling, patient scope, and snapshot integration.
 */
class InsightsReadinessTest {
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
        // active patients 1 and 2 with owners linked/unlinked scenarios
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
            ownerId = 1L, // linked
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
            ownerId = null, // unlinked -> should count for UnlinkedOwner
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
        database.ownerQueries.insertWithId(
            id = 1L,
            name = "Owner One",
            email = null,
            phone = null,
            address = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun insertConsultation(
        id: Long,
        patientId: Long,
        date: LocalDate,
        vetName: String?,
        isActive: Boolean = true,
    ) {
        database.consultationQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            subjective = "s",
            objective = "o",
            assessment = "a",
            plan = "p",
            vetName = vetName,
            nextVisitDate = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun insertReproduction(
        id: Long,
        patientId: Long,
        eventType: String,
        date: LocalDate,
        vetName: String? = "Dr Vet",
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
            vetName = vetName,
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
        recipientMares: String?,
        vetName: String? = "Dr Vet",
        isActive: Boolean = true,
    ) {
        database.embryoTransferQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            embryoCount = 1L,
            recipientMares = recipientMares,
            vetName = vetName,
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
        ovaryStatus: String? = null,
        uterineStatus: String? = null,
        follicleSizeMm: Double? = null,
        leftOvaryStatus: String? = null,
        rightOvaryStatus: String? = null,
        leftFollicleSizeMm: Double? = null,
        rightFollicleSizeMm: Double? = null,
        uterineEdema: String? = null,
        uterineLiquid: Boolean? = null,
        uterineLiquidDescription: String? = null,
        uterusDescription: String? = null,
        isActive: Boolean = true,
    ) {
        database.ultrasoundQueries.insertWithId(
            id = id,
            patientId = patientId,
            date = date,
            ovaryStatus = ovaryStatus,
            uterineStatus = uterineStatus,
            follicleSizeMm = follicleSizeMm,
            leftOvaryStatus = leftOvaryStatus,
            rightOvaryStatus = rightOvaryStatus,
            leftFollicleSizeMm = leftFollicleSizeMm,
            rightFollicleSizeMm = rightFollicleSizeMm,
            uterineEdema = uterineEdema,
            uterineLiquid = uterineLiquid,
            uterineLiquidDescription = uterineLiquidDescription,
            uterusDescription = uterusDescription,
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
    fun `unknown reproduction category counts and drills to affected rows only`() {
        // Known canonical should not count, unknown -> Other should count
        insertReproduction(1, 1L, "Breeding", inRange) // known
        insertReproduction(2, 1L, "breeding", inRange) // legacy known -> not count
        insertReproduction(3, 1L, "Pregnancy Check", inRange) // known
        insertReproduction(4, 1L, "unknownXYZ", inRange) // unknown -> count
        insertReproduction(5, 1L, "Mystery", inRange) // unknown -> count
        insertReproduction(6, 1L, "unknownXYZ", outOfRange, isActive = true) // out of range -> exclude
        insertReproduction(7, 1L, "unknownXYZ", inRange, isActive = false) // soft-deleted -> exclude
        insertReproduction(8, 3L, "unknownXYZ", inRange) // inactive patient -> exclude

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val counts = repository.getDataIssueCounts(filter)
        val unknown = counts.find { it.type == InsightsDataIssueType.UnknownReproductionCategory }?.count ?: 0
        assertEquals(2, unknown, "expected 2 unknown reproduction rows in range")

        val refs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.UnknownReproductionCategory)
        assertEquals(setOf(4L, 5L), refs.map { it.recordId }.toSet())
        assertTrue(refs.all { it.recordId in setOf(4L, 5L) })
        // Verify drill via InsightsDrillDown with dataIssueType
        val drill = InsightsDrillDown(from = inRangeFrom, to = inRangeTo, patientId = null, dataIssueType = InsightsDataIssueType.UnknownReproductionCategory)
        val drillRefs = repository.getRecordRefs(drill)
        assertEquals(refs.map { it.recordId }.toSet(), drillRefs.map { it.recordId }.toSet())
    }

    @Test
    fun `missing vet name counts across types and respects blank whitespace and scope`() {
        // Consultation with vet null, blank, whitespace -> should count
        insertConsultation(1, 1L, inRange, null) // missing
        insertConsultation(2, 1L, inRange, "") // missing
        insertConsultation(3, 1L, inRange, "   ") // whitespace -> missing via LENGTH(TRIM)=0
        insertConsultation(4, 1L, inRange, "Dr Vet") // not missing
        insertConsultation(5, 1L, outOfRange, null) // out of range exclude
        insertConsultation(6, 1L, inRange, null, isActive = false) // soft-deleted exclude
        insertConsultation(7, 3L, inRange, null) // inactive patient exclude
        // Also test another table: Dentistry
        database.dentistryQueries.insertWithId(
            id = 10L,
            patientId = 1L,
            date = inRange,
            findings = "f",
            treatment = "t",
            nextDueDate = null,
            vetName = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dentistryQueries.insertWithId(
            id = 11L,
            patientId = 1L,
            date = inRange,
            findings = "f",
            treatment = "t",
            nextDueDate = null,
            vetName = "Dr Vet",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val counts = repository.getDataIssueCounts(filter)
        val missing = counts.find { it.type == InsightsDataIssueType.MissingVetName }?.count ?: 0
        // 3 consultations + 1 dentistry =4
        assertEquals(4, missing)

        val refs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.MissingVetName)
        assertEquals(4, refs.size)
        assertEquals(setOf(1L, 2L, 3L, 10L), refs.map { it.recordId }.toSet())
        // Check patient scope: only patient 1 has missing, patient 2 has none
        val p1 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 1L)
        val p1Counts = repository.getDataIssueCounts(p1)
        assertEquals(4, p1Counts.find { it.type == InsightsDataIssueType.MissingVetName }?.count ?: 0)
        val p2 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 2L)
        val p2Counts = repository.getDataIssueCounts(p2)
        assertEquals(0, p2Counts.find { it.type == InsightsDataIssueType.MissingVetName }?.count ?: 0)

        // Verify drill via InsightsDrillDown
        val drill = InsightsDrillDown(from = inRangeFrom, to = inRangeTo, patientId = null, dataIssueType = InsightsDataIssueType.MissingVetName)
        val drillRefs = repository.getRecordRefs(drill)
        assertEquals(refs.map { it.recordId }.toSet(), drillRefs.map { it.recordId }.toSet())
    }

    @Test
    fun `unlinked owner counts activity rows where patient ownerId null`() {
        // Patient 1 has owner 1 -> not counted, patient 2 has no owner -> counted
        insertConsultation(1, 1L, inRange, "Dr Vet") // linked -> not counted
        insertConsultation(2, 2L, inRange, "Dr Vet") // unlinked -> counted
        insertConsultation(3, 2L, outOfRange, "Dr Vet") // out of range -> exclude
        insertConsultation(4, 2L, inRange, "Dr Vet", isActive = false) // soft-deleted -> exclude
        insertConsultation(5, 3L, inRange, "Dr Vet") // inactive patient -> exclude
        // Also weight for unlinked owner should count (weight has no vet but still activity)
        database.weightQueries.insertWithId(
            id = 20L,
            patientId = 2L,
            weightKg = 500.0,
            date = inRange,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val counts = repository.getDataIssueCounts(filter)
        val unlinked = counts.find { it.type == InsightsDataIssueType.UnlinkedOwner }?.count ?: 0
        // 1 consultation (id2) + 1 weight (20) =2
        assertEquals(2, unlinked)

        val refs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.UnlinkedOwner)
        assertEquals(setOf(2L, 20L), refs.map { it.recordId }.toSet())

        // Scoped: patient 2 should have 2, patient 1 has 0
        val p2 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 2L)
        assertEquals(2, repository.getDataIssueCounts(p2).find { it.type == InsightsDataIssueType.UnlinkedOwner }?.count ?: 0)
        val p1 = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = 1L)
        assertEquals(0, repository.getDataIssueCounts(p1).find { it.type == InsightsDataIssueType.UnlinkedOwner }?.count ?: 0)
    }

    @Test
    fun `free-text embryo recipient counts non-empty recipientMares`() {
        insertEmbryo(1, 1L, inRange, "Mare A, Mare B") // free-text -> count
        insertEmbryo(2, 1L, inRange, "   ") // whitespace -> not count (trim blank)
        insertEmbryo(3, 1L, inRange, null) // null -> not count
        insertEmbryo(4, 1L, inRange, "") // empty -> not count
        insertEmbryo(5, 1L, outOfRange, "Mare X") // out of range -> exclude
        insertEmbryo(6, 1L, inRange, "Mare Y", isActive = false) // soft-deleted -> exclude
        insertEmbryo(7, 3L, inRange, "Mare Z") // inactive patient -> exclude

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val counts = repository.getDataIssueCounts(filter)
        val free = counts.find { it.type == InsightsDataIssueType.FreeTextEmbryoRecipient }?.count ?: 0
        assertEquals(1, free)

        val refs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.FreeTextEmbryoRecipient)
        assertEquals(listOf(1L), refs.map { it.recordId })
        assertEquals("EMBRYO_TRANSFER", refs.single().recordType.wireName)
    }

    @Test
    fun `incomplete ultrasound counts when structured fields all missing`() {
        // Complete ultrasound (has structured data) -> not counted
        insertUltrasound(1, 1L, inRange, ovaryStatus = "Normal", uterineStatus = "Normal")
        // Incomplete: all structured null -> counted
        insertUltrasound(2, 1L, inRange) // all null -> counted
        // Incomplete with whitespace as empty -> counted
        insertUltrasound(3, 1L, inRange, ovaryStatus = "   ", uterineStatus = "   ")
        // Partial data: one structured field present -> not counted (since not all missing)
        insertUltrasound(4, 1L, inRange, follicleSizeMm = 20.0)
        // Out of range / inactive / inactive patient -> exclude
        insertUltrasound(5, 1L, outOfRange) // out of range
        insertUltrasound(6, 1L, inRange, isActive = false) // soft-deleted
        insertUltrasound(7, 3L, inRange) // inactive patient

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val counts = repository.getDataIssueCounts(filter)
        val incomplete = counts.find { it.type == InsightsDataIssueType.IncompleteUltrasoundData }?.count ?: 0
        // 2 and 3 are incomplete -> 2
        assertEquals(2, incomplete)

        val refs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.IncompleteUltrasoundData)
        assertEquals(setOf(2L, 3L), refs.map { it.recordId }.toSet())
    }

    @Test
    fun `readiness snapshot integrates with GetInsightsDashboardUseCase golden`() {
        // Seed a mix: 1 unknown reproduction, 1 missing vet, 1 unlinked owner activity, 1 free-text embryo, 1 incomplete ultrasound
        insertReproduction(1, 1L, "unknownXYZ", inRange)
        insertConsultation(10, 1L, inRange, null) // missing vet
        // Use patient 2 for unlinked owner (already unlinked)
        insertConsultation(11, 2L, inRange, "Dr Vet") // will be counted as unlinked owner (since patient 2 has no owner)
        insertEmbryo(20, 1L, inRange, "Free Mare")
        insertUltrasound(30, 1L, inRange) // incomplete

        // Also need at least one activity for overview
        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val useCase = GetInsightsDashboardUseCase(repository) { LocalDate(2025, 1, 31) }
        val snap = useCase(filter)

        // Each type should have 1 except missing vet and unlinked may have more? Let's count
        // unknown: 1
        // missing vet: 1 (consultation 10)
        // unlinked: 1 (consultation 11) -> note consultation 10 is patient 1 linked, not counted for unlinked, but consultation 11 is unlinked
        // free-text: 1
        // incomplete: 1
        // So total 5, but missing vet also includes? check if ultrasound missing vet? No, ultrasound vet is "Dr Vet" so not missing.
        // So we expect 5 distinct issue types with count 1 each, total 5
        assertEquals(5, snap.totalIssues)
        assertEquals(5, snap.dataIssues.size)
        assertEquals(1, snap.issuesByType[InsightsDataIssueType.UnknownReproductionCategory])
        assertEquals(1, snap.issuesByType[InsightsDataIssueType.MissingVetName])
        assertEquals(1, snap.issuesByType[InsightsDataIssueType.UnlinkedOwner])
        assertEquals(1, snap.issuesByType[InsightsDataIssueType.FreeTextEmbryoRecipient])
        assertEquals(1, snap.issuesByType[InsightsDataIssueType.IncompleteUltrasoundData])

        // Verify definitions contain "missing for analysis"
        for (type in InsightsDataIssueType.entries) {
            assertTrue(
                type.definition.contains("missing for analysis") || type.missingForAnalysisLabel.contains("missing for analysis"),
                "definition for $type should contain missing for analysis",
            )
        }

        // Verify no quality score
        // Snapshot has no field like qualityScore
        assertTrue(snap::class.members.none { it.name == "qualityScore" })
    }

    @Test
    fun `each issue drill-down via useCase filter isolates only affected rows`() {
        // Setup: 2 unknown repro, 1 missing vet, 1 freeText
        insertReproduction(1, 1L, "unknownA", inRange)
        insertReproduction(2, 1L, "unknownB", inRange)
        insertReproduction(3, 1L, "Breeding", inRange) // known, not counted
        insertConsultation(10, 1L, inRange, null)
        insertEmbryo(20, 1L, inRange, "Mare Free")

        val filter = InsightsFilter(from = inRangeFrom, to = inRangeTo, patientId = null)
        val unknownRefs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.UnknownReproductionCategory)
        assertEquals(2, unknownRefs.size)
        assertTrue(unknownRefs.all { it.recordType.wireName == "REPRODUCTION_EVENT" })

        val missingRefs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.MissingVetName)
        assertEquals(1, missingRefs.size)
        assertEquals(10L, missingRefs.single().recordId)

        val freeRefs = repository.getDataIssueRecordRefs(filter, InsightsDataIssueType.FreeTextEmbryoRecipient)
        assertEquals(1, freeRefs.size)
        assertEquals(20L, freeRefs.single().recordId)

        // Ensure no overlap between issue types (each filters distinct)
        assertTrue(
            unknownRefs
                .map { it.recordId }
                .toSet()
                .intersect(missingRefs.map { it.recordId }.toSet())
                .isEmpty(),
        )
    }
}
