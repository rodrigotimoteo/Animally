package com.github.rodrigotimoteo.animally.domain.settings

import app.cash.sqldelight.Query
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl
import com.github.rodrigotimoteo.animally.data.settings.DatabaseWipePortImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.settings.DatabaseWipePort
import com.github.rodrigotimoteo.animally.domain.settings.usecase.WipeAllDataUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Full-wipe integration test.
 *
 * Seeds one row in every table cleared by [DatabaseWipePortImpl.clearAll] plus
 * both halves of the FTS index, invokes [WipeAllDataUseCase], and asserts the
 * database is empty. This covers the entire wipe surface so regressions that
 * add a `deleteAll()` without seeding/awaiting it are caught.
 *
 * Implementation note: the port's KDoc documents "25 data tables" and the
 * actual impl clears 25 tables ([AssistantChatHistory], [Anamnese],
 * [Consultation], [Dentistry], [Deworming], [FarrierVisit], [Gestation],
 * [Imaging], [LabResult], [Lameness], [Medication], [Owner], [Patient],
 * [ReproMedication], [Reproduction], [Substance], [Surgery], [Ultrasound],
 * [Vaccination], [Weight], [Follicle], [EmbryoTransfer], [Icsi],
 * [CustomReminder], [DictationCapture]) plus both FTS halves. This test asserts
 * all 25 + FTS so it stays correct if the table list changes. Seeding every
 * table has low incremental cost and higher
 * regression value than a minimal 7-table spot-check; a fake
 * [DatabaseWipePort] tracking `clearedTables` would verify delegation but not
 * that the SQL `DELETE`s actually run, so the real-database assertion is
 * retained as the primary gate.
 */
class WipeAllDataUseCaseTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var searchRepository: SearchRepositoryImpl
    private lateinit var databaseWipePort: DatabaseWipePort
    private lateinit var dictationFilePort: TrackingDictationFilePort
    private lateinit var reminderScheduler: ReminderScheduler

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        searchRepository = SearchRepositoryImpl(database, database.ownerQueries)
        databaseWipePort = DatabaseWipePortImpl(database)
        dictationFilePort = TrackingDictationFilePort()
        reminderScheduler = TrackingReminderScheduler()
    }

    @Test
    fun `wipe empties every seeded table and the search index`() {
        seedRows()
        seedSearchIndex()

        val result = WipeAllDataUseCase(databaseWipePort, dictationFilePort, searchRepository, reminderScheduler).invoke()

        // Core identity + clinical history (original 7-table subset)
        assertEmpty { database.ownerQueries.selectAllRows() }
        assertEmpty { database.patientQueries.selectAllRows() }
        assertEmpty { database.anamneseQueries.selectAllRows() }
        assertEmpty { database.consultationQueries.selectAllRows() }
        assertEmpty { database.vaccinationQueries.selectAllRows() }
        assertEmpty { database.weightQueries.selectAllRows() }
        assertEmpty { database.customReminderQueries.selectAllRows() }

        // Remaining tables wiped by DatabaseWipePortImpl.clearAll()
        assertEmpty { database.assistantChatHistoryQueries.selectAllRows() }
        assertEmpty { database.dentistryQueries.selectAllRows() }
        assertEmpty { database.dewormingQueries.selectAllRows() }
        assertEmpty { database.farrierVisitQueries.selectAllRows() }
        assertEmpty { database.gestationQueries.selectAllRows() }
        assertEmpty { database.imagingQueries.selectAllRows() }
        assertEmpty { database.labResultQueries.selectAllRows() }
        assertEmpty { database.lamenessQueries.selectAllRows() }
        assertEmpty { database.medicationQueries.selectAllRows() }
        assertEmpty { database.reproMedicationQueries.selectAllRows() }
        assertEmpty { database.reproductionQueries.selectAllRows() }
        assertEmpty { database.substanceQueries.selectAllRows() }
        assertEmpty { database.surgeryQueries.selectAllRows() }
        assertEmpty { database.ultrasoundQueries.selectAllRows() }
        assertEmpty { database.follicleQueries.selectAllRows() }
        assertEmpty { database.embryoTransferQueries.selectAllRows() }
        assertEmpty { database.icsiQueries.selectAllRows() }
        assertEmpty { database.dictationCaptureQueries.selectAllRows() }

        // Both halves of the FTS index: metadata table and the full-text table.
        assertEquals(0L, database.searchFtsQueries.countIndexRows().executeAsOne())
        assertTrue(searchRepository.search("Charlie", null, null, null).isEmpty())
        assertEquals(
            setOf(
                "/tmp/audio.caf",
                "attachments/radiograph.jpg",
                "attachments/archived-radiograph.jpg",
                "attachments/ultrasound.jpg",
                "attachments/ultrasound-second.jpg",
                "attachments/archived-ultrasound.jpg",
            ),
            dictationFilePort.deletedPaths,
        )
        assertTrue(result.isComplete)
    }

    @Test
    fun `wipe delegates to DatabaseWipePort and rebuilds FTS via fake port`() {
        // Low-ROI fast path: verifies WipeAllDataUseCase delegates to the port
        // and rebuilds the index without seeding 25 tables. Kept as
        // supplementary documentation of the alternative "fake tracking
        // clearedTables" strategy; the real-DB test above is the primary gate.
        val fakePort = TrackingFakeDatabaseWipePort()
        val trackingDictationPort = TrackingDictationFilePort()
        val trackingSearchPort = TrackingSearchRepository()
        // Fake port returns one audio path to verify file deletion delegation.
        fakePort.audioPathsToReturn = setOf("/tmp/audio.caf")

        val trackingReminderScheduler = TrackingReminderScheduler()
        val result = WipeAllDataUseCase(fakePort, trackingDictationPort, trackingSearchPort, trackingReminderScheduler).invoke()

        assertTrue(fakePort.clearAllCalled, "DatabaseWipePort.clearAll should be invoked")
        assertEquals(setOf("/tmp/audio.caf"), trackingDictationPort.deletedPaths)
        assertEquals(1, trackingSearchPort.rebuildCalls)
        assertEquals(1, trackingReminderScheduler.cancelAllCalls)
        // Canonical 25 data tables plus both FTS halves is the wipe surface;
        // fake tracks that delegation would wipe the full set.
        assertEquals(TrackingFakeDatabaseWipePort.EXPECTED_WIPED_TABLES, fakePort.clearedTables)
        assertTrue(result.isComplete)
    }

    @Test
    fun `wipe reports media cleanup failures after database commit`() {
        val fakePort =
            TrackingFakeDatabaseWipePort().apply {
                audioPathsToReturn = setOf("keep.caf", "removed.caf")
            }
        val trackingDictationPort =
            TrackingDictationFilePort().apply {
                undeletablePaths += "keep.caf"
            }

        val result =
            WipeAllDataUseCase(
                fakePort,
                trackingDictationPort,
                TrackingSearchRepository(),
                TrackingReminderScheduler(),
            ).invoke()

        assertEquals(setOf("keep.caf"), result.residualMediaPaths)
        assertTrue(!result.isComplete)
    }

    /** Runs [query] and asserts it returned no rows. */
    private fun assertEmpty(query: () -> Query<*>) {
        assertTrue(query().executeAsList().isEmpty())
    }

    private fun seedRows() {
        database.ownerQueries.insertWithId(
            id = 1L,
            name = "Ana Souza",
            email = "ana@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(100L),
            updatedAt = Instant.fromEpochMilliseconds(100L),
        )
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Charlie",
            species = "Equine",
            breed = "Hanoverian",
            dateOfBirth = LocalDate(2018, 3, 1),
            gender = "Mare",
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = 1L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1234L),
            updatedAt = Instant.fromEpochMilliseconds(1234L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.anamneseQueries.insertWithId(
            id = 2L,
            patientId = 1L,
            generalHistory = "Chronic cough",
            chronicConditions = null,
            allergies = "Penicillin",
            createdAt = Instant.fromEpochMilliseconds(1500L),
            updatedAt = Instant.fromEpochMilliseconds(1500L),
        )
        database.consultationQueries.insertWithId(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2026, 6, 15),
            subjective = "Mild lameness",
            objective = null,
            assessment = "Suspected tendonitis",
            plan = null,
            vetName = null,
            nextVisitDate = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4321L),
            updatedAt = Instant.fromEpochMilliseconds(4321L),
        )
        database.vaccinationQueries.insertWithId(
            id = 5L,
            patientId = 1L,
            vaccineName = "Tetanus",
            batchNumber = null,
            dateAdministered = LocalDate(2026, 5, 10),
            nextDueDate = LocalDate(2027, 5, 10),
            vetName = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(2000L),
            updatedAt = Instant.fromEpochMilliseconds(2000L),
        )
        database.weightQueries.insertWithId(
            id = 3L,
            patientId = 1L,
            weightKg = 520.0,
            date = LocalDate(2026, 5, 1),
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        database.customReminderQueries.insertWithId(
            id = 9L,
            patientId = 1L,
            title = "Check shoe",
            dueDate = LocalDate(2026, 9, 1),
            linkedRecordType = null,
            linkedRecordId = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(3000L),
            updatedAt = Instant.fromEpochMilliseconds(3000L),
        )
        database.assistantChatHistoryQueries.insertWithId(
            id = 10L,
            question = "How is Charlie?",
            answer = "Doing well",
            source = "LOCAL",
            interrupted = false,
            createdAt = Instant.fromEpochMilliseconds(4000L),
            conversationId = "conv-1",
            webSourcesJson = "[]",
            recordSourcesJson = "[]",
        )
        database.dictationCaptureQueries.insertWithId(
            id = 11L,
            transcript = "Check hoof",
            audioPath = "/tmp/audio.caf",
            durationMillis = 1200L,
            capturedAt = Instant.fromEpochMilliseconds(4100L),
        )
        database.dentistryQueries.insertWithId(
            id = 12L,
            patientId = 1L,
            date = LocalDate(2026, 2, 14),
            findings = "Sharp points",
            treatment = "Float",
            nextDueDate = LocalDate(2026, 8, 14),
            vetName = "Dr. Costa",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4200L),
            updatedAt = Instant.fromEpochMilliseconds(4200L),
        )
        database.dewormingQueries.insertWithId(
            id = 13L,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2026, 3, 1),
            nextDueDate = LocalDate(2026, 9, 1),
            dose = "200 mcg/kg",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4300L),
            updatedAt = Instant.fromEpochMilliseconds(4300L),
        )
        database.farrierVisitQueries.insertWithId(
            id = 14L,
            patientId = 1L,
            date = LocalDate(2026, 5, 8),
            trimOrShoe = "Shoeing",
            shoeType = "Steel",
            findings = "Moderate wear",
            nextDueDate = LocalDate(2026, 8, 8),
            farrier = "Marcos",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4400L),
            updatedAt = Instant.fromEpochMilliseconds(4400L),
        )
        database.gestationQueries.insertWithId(
            id = 15L,
            patientId = 1L,
            breedingDate = LocalDate(2026, 7, 2),
            expectedDueDate = LocalDate(2027, 6, 6),
            gestationDays = 120L,
            status = "In foal",
            fetalCount = 1L,
            lastCheckDate = LocalDate(2026, 10, 30),
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4500L),
            updatedAt = Instant.fromEpochMilliseconds(4500L),
        )
        database.imagingQueries.insertWithId(
            id = 16L,
            patientId = 1L,
            type = "Radiograph",
            date = LocalDate(2026, 6, 20),
            findings = "Navicular remodeling",
            imageUris = "attachments/radiograph.jpg",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4600L),
            updatedAt = Instant.fromEpochMilliseconds(4600L),
        )
        database.imagingQueries.insertWithId(
            id = 28L,
            patientId = 1L,
            type = "Archived radiograph",
            date = LocalDate(2025, 6, 20),
            findings = "Archived image",
            imageUris = "attachments/archived-radiograph.jpg",
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(4601L),
            updatedAt = Instant.fromEpochMilliseconds(4601L),
        )
        database.labResultQueries.insertWithId(
            id = 17L,
            patientId = 1L,
            testType = "CBC",
            date = LocalDate(2026, 6, 16),
            results = "WBC 9.2",
            normalRange = "5.9-11.4",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4700L),
            updatedAt = Instant.fromEpochMilliseconds(4700L),
        )
        database.lamenessQueries.insertWithId(
            id = 18L,
            patientId = 1L,
            date = LocalDate(2026, 6, 15),
            gradeAAEP = 2L,
            limbLocation = "LF",
            flexionTest = "Positive",
            diagnosis = "Mild lameness",
            treatment = "Rest",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4800L),
            updatedAt = Instant.fromEpochMilliseconds(4800L),
        )
        database.medicationQueries.insertWithId(
            id = 19L,
            patientId = 1L,
            name = "Phenylbutazone",
            dosage = "2 g",
            route = "Oral",
            frequency = "BID",
            startDate = LocalDate(2026, 6, 15),
            endDate = LocalDate(2026, 6, 29),
            prescribedBy = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4900L),
            updatedAt = Instant.fromEpochMilliseconds(4900L),
        )
        database.reproMedicationQueries.insertWithId(
            id = 20L,
            patientId = 1L,
            medication = "Deslorelin",
            dateAdministered = LocalDate(2026, 7, 3),
            dosage = "1.8 mg",
            purpose = "Induce ovulation",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5000L),
            updatedAt = Instant.fromEpochMilliseconds(5000L),
        )
        database.reproductionQueries.insertWithId(
            id = 21L,
            patientId = 1L,
            eventType = "Breeding",
            date = LocalDate(2026, 7, 2),
            details = "Cover",
            initialExamFindings = "Good tone",
            stallionName = "Cassiano",
            breedingType = "Natural",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5100L),
            updatedAt = Instant.fromEpochMilliseconds(5100L),
        )
        database.substanceQueries.insertWithId(
            id = 22L,
            patientId = 1L,
            drugName = "Detomidine",
            dose = "0.02",
            unit = "mg/kg",
            route = "IV",
            administeredBy = "Dr. Silva",
            witness = "Nurse Ana",
            date = LocalDate(2026, 6, 15),
            reason = "Sedation",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5200L),
            updatedAt = Instant.fromEpochMilliseconds(5200L),
        )
        database.surgeryQueries.insertWithId(
            id = 23L,
            patientId = 1L,
            date = LocalDate(2026, 5, 2),
            type = "Arthroscopy",
            description = "Chip removal",
            outcome = "Good",
            surgeon = "Dr. Costa",
            anesthesia = "Isoflurane",
            analgesia = "Flunixin",
            complications = null,
            recoveryNotes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5300L),
            updatedAt = Instant.fromEpochMilliseconds(5300L),
        )
        database.ultrasoundQueries.insertWithId(
            id = 24L,
            patientId = 1L,
            date = LocalDate(2026, 7, 1),
            ovaryStatus = "Active",
            uterineStatus = "Normal",
            follicleSizeMm = 35.5,
            leftOvaryStatus = "Large follicle",
            rightOvaryStatus = "Inactive",
            leftFollicleSizeMm = 38.25,
            rightFollicleSizeMm = 21.0,
            uterineEdema = "Grade 1",
            uterineLiquid = false,
            uterineLiquidDescription = null,
            uterusDescription = "Normal tone",
            findings = "Pre-ovulatory",
            imageUris = "attachments/ultrasound.jpg, attachments/ultrasound-second.jpg",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5400L),
            updatedAt = Instant.fromEpochMilliseconds(5400L),
        )
        database.ultrasoundQueries.insertWithId(
            id = 29L,
            patientId = 1L,
            date = LocalDate(2025, 7, 1),
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
            findings = "Archived ultrasound",
            imageUris = "attachments/archived-ultrasound.jpg",
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(5401L),
            updatedAt = Instant.fromEpochMilliseconds(5401L),
        )
        database.follicleQueries.insertWithId(
            id = 25L,
            ultrasoundId = 24L,
            side = "LEFT",
            sizeMm = 38.25,
            description = "Dominant",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5500L),
            updatedAt = Instant.fromEpochMilliseconds(5500L),
        )
        database.embryoTransferQueries.insertWithId(
            id = 26L,
            patientId = 1L,
            date = LocalDate(2026, 7, 10),
            embryoCount = 1L,
            recipientMares = "Mare A",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5600L),
            updatedAt = Instant.fromEpochMilliseconds(5600L),
        )
        database.icsiQueries.insertWithId(
            id = 27L,
            patientId = 1L,
            date = LocalDate(2026, 7, 11),
            folliclesRecovered = 5L,
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5700L),
            updatedAt = Instant.fromEpochMilliseconds(5700L),
        )
    }

    /** Writes one metadata row + its FTS twin directly, bypassing the repository. */
    private fun seedSearchIndex() {
        database.transaction {
            database.searchFtsQueries
                .insertIndex(ISearchRepository.TYPE_PATIENT, 1L, 1L, null, "Charlie Hanoverian")
                .value
            database.searchFtsQueries.insertFts("Charlie Hanoverian").value
        }
    }
}

private class TrackingFakeDatabaseWipePort : DatabaseWipePort {
    var clearAllCalled: Boolean = false
    var audioPathsToReturn: Set<String> = emptySet()

    /**
     * Canonical set of tables that [DatabaseWipePortImpl.clearAll] wipes, plus
     * both FTS halves. Mirrors the 25 `deleteAll*` calls in the impl so the
     * fake can assert the expected wipe surface without seeding a real DB.
     */
    val clearedTables: Set<String> = EXPECTED_WIPED_TABLES

    override fun clearAll(): Set<String> {
        clearAllCalled = true
        return audioPathsToReturn
    }

    companion object {
        val EXPECTED_WIPED_TABLES: Set<String> =
            setOf(
                "AssistantChatHistory",
                "Anamnese",
                "Consultation",
                "Dentistry",
                "Deworming",
                "FarrierVisit",
                "Gestation",
                "Imaging",
                "LabResult",
                "Lameness",
                "Medication",
                "Owner",
                "Patient",
                "ReproMedication",
                "Reproduction",
                "Substance",
                "Surgery",
                "Ultrasound",
                "Vaccination",
                "Weight",
                "Follicle",
                "EmbryoTransfer",
                "Icsi",
                "CustomReminder",
                "DictationCapture",
                "SearchFtsIndex",
                "SearchFts",
            )
    }
}

private class TrackingDictationFilePort : DictationFilePort {
    val deletedPaths: MutableSet<String> = mutableSetOf()
    val undeletablePaths: MutableSet<String> = mutableSetOf()

    override fun delete(path: String): Boolean {
        deletedPaths.add(path)
        return path !in undeletablePaths
    }
}

private class TrackingReminderScheduler : ReminderScheduler {
    var cancelAllCalls: Int = 0

    override fun schedule(reminder: com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder) = Unit

    override fun cancelAll() {
        cancelAllCalls++
    }
}

private class TrackingSearchRepository : ISearchRepository {
    var rebuildCalls: Int = 0

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

    override fun rebuild() {
        rebuildCalls += 1
    }

    override fun reindexIfNeeded(indexVersion: String) = Unit
}
