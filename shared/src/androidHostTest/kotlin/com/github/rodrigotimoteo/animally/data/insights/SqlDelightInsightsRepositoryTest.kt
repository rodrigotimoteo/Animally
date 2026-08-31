package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SqlDelightInsightsRepositoryTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SqlDelightInsightsRepository

    private val now = Instant.fromEpochMilliseconds(0L)
    private val inRange = LocalDate(2025, 1, 15)
    private val outOfRange = LocalDate(2024, 12, 1)
    private val earliest = LocalDate(2025, 1, 5)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = SqlDelightInsightsRepository(database)
        seedPatients()
    }

    private fun seedPatients() {
        // active patients 1 and 2, inactive patient 3
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

    private fun seedAllIncludedTypes(
        patientId: Long,
        date: LocalDate,
        isActive: Boolean = true,
        idOffset: Long = 0L,
    ) = com.github.rodrigotimoteo.animally.fixtures.InsightsTestFixtures
        .seedAllIncludedTypes(database, patientId, date, isActive, idOffset, now)

    @Test
    fun `when seeding all included types then buckets count 17 and sum matches total`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange)

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        assertEquals(17, buckets.size, "expected 17 distinct (date,patient,type) buckets")
        val total = buckets.sumOf { it.count }
        assertEquals(17, total)

        // each type appears exactly once
        val types = buckets.map { it.recordType }.toSet()
        assertEquals(17, types.size)
        assertTrue(types.contains(RecordType.Consultation))
        assertTrue(types.contains(RecordType.Icsi))
        assertTrue(types.contains(RecordType.EmbryoTransfer))
        assertTrue(types.contains(RecordType.ControlledSubstance))
        assertTrue(types.contains(RecordType.ReproductionEvent))
        assertTrue(types.contains(RecordType.ReproMedication))
        // sum of per-type counts equals total (already checked, but also verify via grouped by type logic)
        val byTypeSum = buckets.groupBy { it.recordType }.values.sumOf { group -> group.sumOf { it.count } }
        assertEquals(total, byTypeSum)
    }

    @Test
    fun `when patient scoped then only that patient rows returned and sum still matches`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        seedAllIncludedTypes(patientId = 2L, date = inRange, idOffset = 100)

        val global = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val globalBuckets = sut.getActivityBuckets(global)
        assertEquals(34, globalBuckets.size) // 17 per patient *2
        assertEquals(34, globalBuckets.sumOf { it.count })

        val scoped = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = 1L)
        val scopedBuckets = sut.getActivityBuckets(scoped)
        assertEquals(17, scopedBuckets.size)
        assertEquals(17, scopedBuckets.sumOf { it.count })
        assertTrue(scopedBuckets.all { it.patientId == 1L })

        val scoped2 = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = 2L)
        val scoped2Buckets = sut.getActivityBuckets(scoped2)
        assertEquals(17, scoped2Buckets.size)
        assertTrue(scoped2Buckets.all { it.patientId == 2L })
    }

    @Test
    fun `soft deleted rows excluded`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, isActive = true, idOffset = 0)
        seedAllIncludedTypes(patientId = 1L, date = inRange, isActive = false, idOffset = 1000)

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        // only active rows count
        assertEquals(17, buckets.size)
        assertEquals(17, buckets.sumOf { it.count })
    }

    @Test
    fun `inactive patient rows excluded`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        seedAllIncludedTypes(patientId = 3L, date = inRange, idOffset = 500)

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        // patient 3 is inactive, should not appear
        assertEquals(17, buckets.size)
        assertTrue(buckets.none { it.patientId == 3L })
    }

    @Test
    fun `out of range rows excluded`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        seedAllIncludedTypes(patientId = 1L, date = outOfRange, idOffset = 700)

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        assertEquals(17, buckets.size)
        // ensure earliest still resolves to outOfRange if we query earliest, but bucket excludes it
        assertTrue(buckets.none { it.date == outOfRange })
    }

    @Test
    fun `null medication start excluded`() {
        // seed normal 17, plus an extra medication with null startDate
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        database.medicationQueries.insertWithId(
            id = 99999L,
            patientId = 1L,
            name = "NullStartMed",
            dosage = "10mg",
            route = null,
            frequency = null,
            startDate = null,
            endDate = null,
            prescribedBy = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        assertEquals(17, buckets.size)
        assertEquals(17, buckets.sumOf { it.count })
        // ensure medication count is exactly 1 (not 2)
        val medBuckets = buckets.filter { it.recordType == RecordType.Medication }
        assertEquals(1, medBuckets.size)
        assertEquals(1, medBuckets.sumOf { it.count })
    }

    @Test
    fun `excluded types gestation anamnese customReminder follicle never contribute`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        // Gestation (longitudinal)
        database.gestationQueries.insertWithId(
            id = 80001L,
            patientId = 1L,
            breedingDate = inRange,
            expectedDueDate = LocalDate(2026, 1, 1),
            gestationDays = 10L,
            status = "Active",
            fetalCount = 1L,
            lastCheckDate = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // Anamnese (no date)
        database.anamneseQueries.insertWithId(
            id = 80002L,
            patientId = 2L,
            generalHistory = "history",
            chronicConditions = null,
            allergies = null,
            createdAt = now,
            updatedAt = now,
        )
        // CustomReminder (dueDate not activity)
        database.customReminderQueries.insertWithId(
            id = 80003L,
            patientId = 1L,
            title = "Reminder",
            dueDate = inRange,
            linkedRecordType = null,
            linkedRecordId = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // Follicle child (ultrasound child) - distinct date to avoid grouping merge
        database.ultrasoundQueries.insertWithId(
            id = 80004L,
            patientId = 1L,
            date = LocalDate(2025, 1, 16),
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
            findings = "parent",
            imageUris = null,
            vetName = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // Need to guarantee we already have an ultrasound row at 80004, but we also had one at 13000
        // Now add follicle referencing it
        database.follicleQueries.insertWithId(
            id = 80005L,
            ultrasoundId = 80004L,
            side = "LEFT",
            sizeMm = 20.0,
            description = "follicle",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        // gestation/anamnese/reminder/follicle should not increase count beyond 17 + 1 extra ultrasound (80004)
        // We already have 17, plus one extra ultrasound parent (80004) -> 18
        // But follicle itself not counted, anamnese/reminder/gestation not counted
        assertEquals(18, buckets.size)
        assertEquals(18, buckets.sumOf { it.count })
        assertTrue(buckets.none { it.recordType == RecordType.Gestation })
        assertTrue(buckets.none { it.recordType == RecordType.Anamnese })
        assertTrue(buckets.none { it.recordType == RecordType.CustomReminder })
    }

    @Test
    fun `selectActivityRecordRefs returns underlying refs ordered and filtered by type`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        // add another consultation on different date within range for same patient
        database.consultationQueries.insertWithId(
            id = 99901L,
            patientId = 1L,
            date = LocalDate(2025, 1, 18),
            subjective = "s",
            objective = "o",
            assessment = "a",
            plan = "p",
            vetName = "Dr. Vet",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val drillAll =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = null,
                recordType = null,
            )
        val refs = sut.getRecordRefs(drillAll)
        assertEquals(18, refs.size) // 17 + 1 extra
        // ordered by date DESC
        assertTrue(refs[0].date >= refs[1].date)
        assertEquals("Star", refs.first { it.recordId == 1000L }.patientName)

        val drillConsult =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = null,
                recordType = RecordType.Consultation,
            )
        val consultRefs = sut.getRecordRefs(drillConsult)
        assertEquals(2, consultRefs.size)
        assertTrue(consultRefs.all { it.recordType == RecordType.Consultation })

        val drillPatient =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = 1L,
                recordType = RecordType.Consultation,
            )
        assertEquals(2, sut.getRecordRefs(drillPatient).size)

        val drillPatient2 =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = 2L,
                recordType = null,
            )
        assertEquals(0, sut.getRecordRefs(drillPatient2).size)
    }

    @Test
    fun `reproduction subtype drill down canonicalises legacy values exactly`() {
        fun insertEvent(
            id: Long,
            patientId: Long,
            type: String,
            date: LocalDate = inRange,
            isActive: Boolean = true,
        ) {
            database.reproductionQueries.insertWithId(
                id = id,
                patientId = patientId,
                eventType = type,
                date = date,
                details = null,
                initialExamFindings = null,
                stallionName = null,
                breedingType = null,
                vetName = null,
                notes = null,
                isActive = isActive,
                createdAt = now,
                updatedAt = now,
            )
        }
        insertEvent(1L, 1L, "Pregnancy Check")
        insertEvent(2L, 1L, "pregnancy_check")
        insertEvent(3L, 1L, "Breeding")
        insertEvent(4L, 2L, "PREGNANCY-CHECK")
        insertEvent(5L, 1L, "Pregnancy Check", isActive = false)
        insertEvent(6L, 3L, "Pregnancy Check")
        insertEvent(7L, 1L, "Pregnancy Check", date = outOfRange)

        val global =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                recordType = RecordType.ReproductionEvent,
                reproductionEventType = ReproductionEventType.PregnancyCheck,
            )
        assertEquals(listOf(4L, 2L, 1L), sut.getRecordRefs(global).map { it.recordId })

        val patient = global.copy(patientId = 1L)
        assertEquals(setOf(1L, 2L), sut.getRecordRefs(patient).map { it.recordId }.toSet())
    }

    @Test
    fun `recordRefs respects soft delete inactive patient and range and null medication`() {
        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        // soft deleted vaccination within range
        database.vaccinationQueries.insertWithId(
            id = 99902L,
            patientId = 1L,
            vaccineName = "SoftDeleted",
            dateAdministered = inRange,
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        // inactive patient
        database.vaccinationQueries.insertWithId(
            id = 99903L,
            patientId = 3L,
            vaccineName = "InactivePatientVac",
            dateAdministered = inRange,
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // out of range
        database.vaccinationQueries.insertWithId(
            id = 99904L,
            patientId = 1L,
            vaccineName = "OutOfRange",
            dateAdministered = outOfRange,
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // null medication (should not appear in refs either)
        database.medicationQueries.insertWithId(
            id = 99905L,
            patientId = 1L,
            name = "NullMed",
            dosage = "5mg",
            route = null,
            frequency = null,
            startDate = null,
            endDate = null,
            prescribedBy = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val drill =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = null,
                recordType = RecordType.Vaccination,
            )
        val vacRefs = sut.getRecordRefs(drill)
        assertEquals(1, vacRefs.size) // only the one within range active patient
        assertTrue(vacRefs.none { it.recordId == 99902L })
        assertTrue(vacRefs.none { it.recordId == 99903L })
        assertTrue(vacRefs.none { it.recordId == 99904L })

        val medDrill =
            InsightsDrillDown(
                from = LocalDate(2025, 1, 10),
                to = LocalDate(2025, 1, 20),
                patientId = null,
                recordType = RecordType.Medication,
            )
        val medRefs = sut.getRecordRefs(medDrill)
        assertEquals(1, medRefs.size)
    }

    @Test
    fun `selectEarliestActivityDate resolves all time range`() {
        assertNull(sut.getEarliestActivityDate(null))

        seedAllIncludedTypes(patientId = 1L, date = inRange, idOffset = 0)
        database.vaccinationQueries.insertWithId(
            id = 99001L,
            patientId = 1L,
            vaccineName = "Early",
            dateAdministered = earliest,
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // out-of-scope patient inactive should not affect earliest
        database.vaccinationQueries.insertWithId(
            id = 99002L,
            patientId = 3L,
            vaccineName = "InactiveEarly",
            dateAdministered = LocalDate(2020, 1, 1),
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // soft-deleted earlier date should not count
        database.vaccinationQueries.insertWithId(
            id = 99003L,
            patientId = 1L,
            vaccineName = "SoftDeletedEarly",
            dateAdministered = LocalDate(2020, 1, 1),
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(earliest, sut.getEarliestActivityDate(null))
        assertEquals(earliest, sut.getEarliestActivityDate(1L))
        assertNull(sut.getEarliestActivityDate(2L)) // patient 2 has no rows
        // add row for patient 2 later
        database.consultationQueries.insertWithId(
            id = 99004L,
            patientId = 2L,
            date = LocalDate(2025, 1, 12),
            subjective = "s",
            objective = "o",
            assessment = "a",
            plan = "p",
            vetName = null,
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        assertEquals(LocalDate(2025, 1, 12), sut.getEarliestActivityDate(2L))
    }

    @Test
    fun `recordType counts sum to total even with multiple patients and dates`() {
        seedAllIncludedTypes(patientId = 1L, date = LocalDate(2025, 1, 11), idOffset = 0)
        seedAllIncludedTypes(patientId = 1L, date = LocalDate(2025, 1, 15), idOffset = 1000)
        seedAllIncludedTypes(patientId = 2L, date = LocalDate(2025, 1, 11), idOffset = 2000)

        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val buckets = sut.getActivityBuckets(filter)
        // 17 types *3 seeds =51 rows, but buckets grouped by date+patient+type => 51 distinct buckets each count 1
        assertEquals(51, buckets.size)
        assertEquals(51, buckets.sumOf { it.count })

        // also test refs sum
        val refs =
            sut.getRecordRefs(
                InsightsDrillDown(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null, recordType = null),
            )
        assertEquals(51, refs.size)
    }
}
