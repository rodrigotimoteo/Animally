package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.data.medication.MedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.medication.usecase.SaveMedicationUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Golden-question regression baseline for RAG retrieval recall.
 *
 * Seeds a deterministic multi-patient fixture covering every indexed record
 * type, builds the index through the REAL production healing pass
 * ([SearchRepositoryImpl.reindexIfNeeded]), and runs natural questions through
 * the REAL retrieval path used by [GenerateRagResponseUseCase]
 * ([SearchUseCase] + enrich/OR-retry). Expectations are order-insensitive:
 * this locks RECALL so ranking changes (BM25, synonym expansion, snippet
 * windows, patient scoping) cannot silently drop records from the retrieved
 * set. Ordering is locked separately in [currentDefaultOrderBella].
 *
 * Known retrieval gaps are pinned as empty expectations ("vaccination",
 * "shod", "embryo transfer") so the synonym-expansion change flips them
 * deliberately instead of silently.
 */
class RagGoldenSetTest {
    private val database = createTestDatabase()
    private val searchRepo = SearchRepositoryImpl(database, database.ownerQueries)
    private val searchUseCase = SearchUseCase(searchRepo)

    private val now = Clock.System.now()

    /** Stable record keys used across golden expectations. */
    private companion object {
        const val PATIENT_THUNDER = "PATIENT#1"
        const val PATIENT_BELLA = "PATIENT#2"
        const val PATIENT_COMET = "PATIENT#3"
        const val OWNER_DANIELA = "OWNER#1"
        const val OWNER_MIGUEL = "OWNER#2"
        const val VACC_INFLUENZA = "VACCINATION#101"
        const val VACC_TETANUS = "VACCINATION#102"
        const val VACC_WEST_NILE = "VACCINATION#103"
        const val DEWORM_IVERMECTIN = "DEWORMING#201"
        const val DEWORM_FENBENDAZOLE = "DEWORMING#202"
        const val FARRIER_SHOEING = "FARRIER_VISIT#301"
        const val GESTATION_ACTIVE = "GESTATION#401"
        const val GESTATION_COMPLETED = "GESTATION#402"
        const val US_THUNDER_FOLLICLE = "ULTRASOUND#501"
        const val US_BELLA_FOLLICLE = "ULTRASOUND#502"
        const val WEIGHT_512 = "WEIGHT#601"
        const val WEIGHT_525 = "WEIGHT#602"
        const val WEIGHT_538 = "WEIGHT#603"
        const val WEIGHT_COMET = "WEIGHT#604"
        const val CONSULT_COLIC = "CONSULTATION#701"
        const val CONSULT_COUGH = "CONSULTATION#702"
        const val LAMENESS_NAVICULAR = "LAMENESS#901"
        const val LAB_CBC = "LAB_RESULT#1001"
        const val LAB_COGGINS = "LAB_RESULT#1002"
        const val SUBSTANCE_DETOMIDINE = "CONTROLLED_SUBSTANCE#1101"
        const val REPRO_BREEDING = "REPRODUCTION_EVENT#1201"
        const val REPRO_HEAT = "REPRODUCTION_EVENT#1202"
        const val REPRO_MEDICATION_DESLORELIN = "REPRO_MEDICATION#1301"
        const val DENTISTRY_FLOATING = "DENTISTRY#1401"
        const val SURGERY_ARTHROSCOPY = "SURGERY#1501"
        const val IMAGING_KNEE = "IMAGING#1601"
        const val ET_BELLA = "EMBRYO_TRANSFER#1701"
        const val ICSI_COMET = "ICSI#1801"
    }

    /** Medication id is assigned by the save path; resolved during seeding. */
    private var medicationId: Long = 0L

    init {
        seedFixture()
    }

    private fun seedFixture() {
        seedOwnersAndPatients()
        seedThunderRecords()
        seedBellaRecords()
        seedCometRecords()
        seedMedicationViaSavePath()
        // Build the whole index through the real production healing pass.
        searchRepo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
    }

    private fun seedOwnersAndPatients() {
        database.ownerQueries.insertWithId(
            id = 1L,
            name = "Daniela Costa",
            email = "daniela@example.com",
            phone = "+351912345678",
            address = "Rua das Flores 12",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.ownerQueries.insertWithId(
            id = 2L,
            name = "Miguel Santos",
            email = "miguel@example.com",
            phone = null,
            address = "Herdade Boa Esperanca",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Thunder",
            species = "Equine",
            breed = "Thoroughbred",
            dateOfBirth = LocalDate(2019, 4, 10),
            gender = "Mare",
            microchipId = "MC123456",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Barn A",
            photoUri = null,
            notes = null,
            ownerId = 1L,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 2L,
            name = "Bella",
            species = "Equine",
            breed = "Lusitano",
            dateOfBirth = LocalDate(2017, 3, 2),
            gender = "Mare",
            microchipId = "MC654321",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Quinta do Vale",
            photoUri = null,
            notes = null,
            ownerId = 1L,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 3L,
            name = "Comet",
            species = "Equine",
            breed = "Quarter Horse",
            dateOfBirth = LocalDate(2020, 6, 15),
            gender = "Gelding",
            microchipId = "MC999888",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Barn B",
            photoUri = null,
            notes = null,
            ownerId = 2L,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun seedThunderRecords() {
        database.vaccinationQueries.insertWithId(
            id = 101L,
            patientId = 1L,
            vaccineName = "Influenza",
            dateAdministered = LocalDate(2026, 1, 15),
            nextDueDate = LocalDate(2027, 1, 15),
            vetName = "Dr. House",
            batchNumber = "FLU-2026-044",
            site = "Neck",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.vaccinationQueries.insertWithId(
            id = 102L,
            patientId = 1L,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate(2026, 2, 10),
            nextDueDate = LocalDate(2028, 2, 10),
            vetName = "Dr. House",
            batchNumber = "TET-2026-002",
            site = "Left shoulder",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dewormingQueries.insertWithId(
            id = 201L,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2026, 3, 5),
            nextDueDate = LocalDate(2026, 9, 5),
            dose = "200 ug/kg",
            vetName = "Dr. House",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.farrierVisitQueries.insertWithId(
            id = 301L,
            patientId = 1L,
            date = LocalDate(2026, 4, 1),
            trimOrShoe = "Shoeing",
            shoeType = "Steel full set",
            findings = "Moderate wear",
            nextDueDate = LocalDate(2026, 7, 1),
            farrier = "John Smith",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.gestationQueries.insertWithId(
            id = 401L,
            patientId = 1L,
            breedingDate = LocalDate(2026, 3, 20),
            expectedDueDate = LocalDate(2027, 2, 24),
            gestationDays = 0,
            status = "Active",
            fetalCount = 1,
            lastCheckDate = LocalDate(2026, 4, 20),
            notes = "AI with stallion Eclipse",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.ultrasoundQueries.insertWithId(
            id = 501L,
            patientId = 1L,
            date = LocalDate(2026, 3, 18),
            ovaryStatus = null,
            uterineStatus = null,
            follicleSizeMm = 35.0,
            leftOvaryStatus = null,
            rightOvaryStatus = null,
            leftFollicleSizeMm = null,
            rightFollicleSizeMm = null,
            uterineEdema = null,
            uterineLiquid = null,
            uterineLiquidDescription = null,
            uterusDescription = null,
            findings = "Dominant follicle 35 mm left ovary",
            imageUris = null,
            vetName = "Dr. House",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        seedWeight(601L, patientId = 1L, weightKg = 512.0, date = LocalDate(2026, 1, 5))
        seedWeight(602L, patientId = 1L, weightKg = 525.5, date = LocalDate(2026, 3, 5))
        seedWeight(603L, patientId = 1L, weightKg = 538.0, date = LocalDate(2026, 5, 5))
        database.consultationQueries.insertWithId(
            id = 701L,
            patientId = 1L,
            date = LocalDate(2026, 2, 20),
            subjective = "Acute colic signs overnight",
            objective = "Elevated heart rate, gut sounds reduced",
            assessment = "Large colon displacement",
            plan = "Referral for colic surgery",
            vetName = "Dr. House",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.lamenessQueries.insertWithId(
            id = 901L,
            patientId = 1L,
            date = LocalDate(2026, 6, 10),
            gradeAAEP = 3,
            limbLocation = "Right forelimb",
            flexionTest = "Positive",
            diagnosis = "Navicular syndrome",
            treatment = "Bute and rest",
            vetName = "Dr. House",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.labResultQueries.insertWithId(
            id = 1001L,
            patientId = 1L,
            testType = "CBC",
            date = LocalDate(2026, 2, 21),
            results = "Elevated fibrinogen",
            normalRange = "100-400 mg/dL",
            vetName = "Dr. House",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.substanceQueries.insertWithId(
            id = 1101L,
            patientId = 1L,
            drugName = "Detomidine",
            dose = "10",
            unit = "mg",
            route = "IV",
            administeredBy = "Dr. House",
            witness = "Nurse Silva",
            date = LocalDate(2026, 2, 20),
            reason = "Colic sedation",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.reproductionQueries.insertWithId(
            id = 1201L,
            patientId = 1L,
            eventType = "Breeding",
            date = LocalDate(2026, 3, 20),
            details = "AI fresh semen",
            initialExamFindings = null,
            stallionName = "Eclipse",
            breedingType = "Fresh cooled",
            vetName = "Dr. Costa",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dentistryQueries.insertWithId(
            id = 1401L,
            patientId = 1L,
            date = LocalDate(2026, 1, 28),
            findings = "Sharp enamel points",
            treatment = "Floating",
            nextDueDate = LocalDate(2026, 7, 28),
            vetName = "Dr. Wilson",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun seedBellaRecords() {
        database.vaccinationQueries.insertWithId(
            id = 103L,
            patientId = 2L,
            vaccineName = "West Nile Virus",
            dateAdministered = LocalDate(2026, 4, 12),
            nextDueDate = LocalDate(2027, 4, 12),
            vetName = "Dr. Wilson",
            batchNumber = "WNV-2026-007",
            site = "Neck",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dewormingQueries.insertWithId(
            id = 202L,
            patientId = 2L,
            product = "Fenbendazole",
            dateAdministered = LocalDate(2026, 5, 20),
            nextDueDate = LocalDate(2026, 11, 20),
            dose = "10 mg/kg",
            vetName = "Dr. Wilson",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.gestationQueries.insertWithId(
            id = 402L,
            patientId = 2L,
            breedingDate = LocalDate(2025, 4, 1),
            expectedDueDate = LocalDate(2026, 3, 8),
            gestationDays = 341,
            status = "Completed",
            fetalCount = 1,
            lastCheckDate = LocalDate(2026, 3, 8),
            notes = "Live filly born",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.ultrasoundQueries.insertWithId(
            id = 502L,
            patientId = 2L,
            date = LocalDate(2026, 5, 18),
            ovaryStatus = null,
            uterineStatus = null,
            follicleSizeMm = 22.0,
            leftOvaryStatus = null,
            rightOvaryStatus = null,
            leftFollicleSizeMm = null,
            rightFollicleSizeMm = null,
            uterineEdema = null,
            uterineLiquid = null,
            uterineLiquidDescription = null,
            uterusDescription = null,
            findings = "Small follicle 22 mm right ovary",
            imageUris = null,
            vetName = "Dr. Wilson",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.labResultQueries.insertWithId(
            id = 1002L,
            patientId = 2L,
            testType = "Coggins ELISA",
            date = LocalDate(2026, 5, 21),
            results = "Negative",
            normalRange = "Negative",
            vetName = "Dr. Wilson",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.reproductionQueries.insertWithId(
            id = 1202L,
            patientId = 2L,
            eventType = "Heat",
            date = LocalDate(2026, 5, 10),
            details = "Follicle 22 mm",
            initialExamFindings = null,
            stallionName = null,
            breedingType = null,
            vetName = "Dr. Costa",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.reproMedicationQueries.insertWithId(
            id = 1301L,
            patientId = 2L,
            medication = "Deslorelin",
            dateAdministered = LocalDate(2026, 5, 12),
            dosage = "1.8 mg",
            purpose = "Induce ovulation",
            vetName = "Dr. Costa",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.imagingQueries.insertWithId(
            id = 1601L,
            patientId = 2L,
            type = "Radiograph",
            date = LocalDate(2026, 6, 1),
            findings = "Mild knee osteoarthritis",
            imageUris = null,
            vetName = "Dr. Wilson",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.embryoTransferQueries.insertWithId(
            id = 1701L,
            patientId = 2L,
            date = LocalDate(2026, 6, 15),
            embryoCount = 1,
            recipientMares = "Recipient mares A and B",
            vetName = "Dr. Costa",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun seedCometRecords() {
        seedWeight(604L, patientId = 3L, weightKg = 480.0, date = LocalDate(2026, 2, 15))
        database.consultationQueries.insertWithId(
            id = 702L,
            patientId = 3L,
            date = LocalDate(2026, 4, 8),
            subjective = "Intermittent dry cough",
            objective = "Normal temperature",
            assessment = "Suspected mild RAO",
            plan = "Rest and monitor",
            vetName = "Dr. Wilson",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.surgeryQueries.insertWithId(
            id = 1501L,
            patientId = 3L,
            date = LocalDate(2026, 4, 20),
            type = "Arthroscopy",
            description = "Chip fragment stifle",
            outcome = "Successful",
            surgeon = "Dr. Mendes",
            anesthesia = "General",
            analgesia = "Phenylbutazone",
            complications = null,
            recoveryNotes = "Stall rest 6 weeks",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.icsiQueries.insertWithId(
            id = 1801L,
            patientId = 3L,
            date = LocalDate(2026, 7, 1),
            folliclesRecovered = 8,
            vetName = "Dr. Mendes",
            notes = "Oocytes recovered",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun seedWeight(
        id: Long,
        patientId: Long,
        weightKg: Double,
        date: LocalDate,
    ) {
        database.weightQueries.insertWithId(
            id = id,
            patientId = patientId,
            weightKg = weightKg,
            date = date,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    /** Medication indexing lives only on the save path, so seed through it. */
    private fun seedMedicationViaSavePath() {
        val saveMedication =
            SaveMedicationUseCase(
                medicationRepository = MedicationRepositoryImpl(database),
                searchRepository = searchRepo,
            )
        medicationId =
            saveMedication(
                Medication(
                    id = 0L,
                    patientId = 1L,
                    name = "Metronidazole",
                    dosage = "500 mg twice daily",
                    route = "Oral",
                    frequency = "Twice daily",
                    startDate = LocalDate(2026, 2, 21),
                    endDate = null,
                    prescribedBy = "Dr. House",
                    notes = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
    }

    // ------------------------------------------------------------------
    // Golden set
    // ------------------------------------------------------------------

    /**
     * One golden question. [expected] keys MUST appear in the retrieved set;
     * [forbidden] keys MUST NOT; when [exact] is set the retrieved key set must
     * equal [expected] precisely.
     */
    private data class Golden(
        val question: String,
        val expected: Set<String>,
        val forbidden: Set<String> = emptySet(),
        val exact: Boolean = false,
    )

    private fun goldenSet(): List<Golden> {
        // Record searchableText never contains the patient name, so bare name
        // queries hit only the PATIENT row; combined questions recover records
        // through the OR retry (locked as-is below).
        return listOf(
            // --- Exact name hits ---
            Golden("Thunder", expected = setOf(PATIENT_THUNDER), exact = true),
            // "tell"/"me"/"about" are filler words, so the OR retry reduces to
            // "thunder*" and hits only the patient row - the conversational
            // lead-in no longer leaks every "me..." record (Mendes,
            // Metronidazole). Previously locked WITH that leak; tightened when
            // the filler list gained the pronouns.
            Golden(
                "Tell me about Thunder",
                expected = setOf(PATIENT_THUNDER),
                exact = true,
            ),
            // --- Breed queries ---
            Golden("Thoroughbred", expected = setOf(PATIENT_THUNDER), exact = true),
            // "horse*" also matches Comet's "Quarter Horse" - locked as-is.
            Golden("Lusitano horse", expected = setOf(PATIENT_BELLA, PATIENT_COMET), exact = true),
            Golden("Quarter Horse", expected = setOf(PATIENT_COMET), exact = true),
            // --- Treatment / drug keywords ---
            Golden("Ivermectin", expected = setOf(DEWORM_IVERMECTIN), exact = true),
            Golden("Metronidazole 500", expected = setOf("MEDICATION#$medicationId"), exact = true),
            Golden("Detomidine sedation", expected = setOf(SUBSTANCE_DETOMIDINE), exact = true),
            Golden("Tetanus booster", expected = setOf(VACC_TETANUS), exact = true),
            Golden("Fenbendazole", expected = setOf(DEWORM_FENBENDAZOLE), exact = true),
            Golden("Deslorelin", expected = setOf(REPRO_MEDICATION_DESLORELIN), exact = true),
            Golden("colic surgery", expected = setOf(CONSULT_COLIC), exact = true),
            Golden("Arthroscopy stifle", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            Golden("Navicular syndrome", expected = setOf(LAMENESS_NAVICULAR), exact = true),
            Golden("RAO", expected = setOf(CONSULT_COUGH), exact = true),
            Golden("osteoarthritis", expected = setOf(IMAGING_KNEE), exact = true),
            // --- Clinical findings / vets / staff ---
            Golden("CBC fibrinogen", expected = setOf(LAB_CBC), exact = true),
            Golden("Coggins", expected = setOf(LAB_COGGINS), exact = true),
            Golden("John Smith", expected = setOf(FARRIER_SHOEING), exact = true),
            Golden(
                "Wilson",
                expected =
                    setOf(
                        VACC_WEST_NILE,
                        DEWORM_FENBENDAZOLE,
                        US_BELLA_FOLLICLE,
                        LAB_COGGINS,
                        IMAGING_KNEE,
                        DENTISTRY_FLOATING,
                        CONSULT_COUGH,
                    ),
                exact = true,
            ),
            // --- Date-window questions (locked in dedicated tests below) ---
            // --- Synonym-prone phrasings ---
            Golden("Is Thunder in foal?", expected = setOf(PATIENT_THUNDER, GESTATION_ACTIVE), exact = true),
            Golden("pregnant", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("gestation", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("Completed", expected = setOf(GESTATION_COMPLETED), exact = true),
            Golden("filly", expected = setOf(GESTATION_COMPLETED), exact = true),
            // Current gaps pinned as empty: synonym expansion must flip these deliberately.
            Golden("vaccination", expected = emptySet(), exact = true),
            Golden("shod", expected = emptySet(), exact = true),
            Golden("embryo transfer", expected = emptySet(), exact = true),
            // --- Owner questions ---
            Golden(
                "Which patients belong to Daniela Costa?",
                expected =
                    setOf(
                        OWNER_DANIELA,
                        REPRO_BREEDING,
                        REPRO_HEAT,
                        REPRO_MEDICATION_DESLORELIN,
                        ET_BELLA, // vetName "Dr. Costa" leaks into the owner question
                    ),
                forbidden = setOf(OWNER_MIGUEL),
                exact = true,
            ),
            Golden("Miguel Santos", expected = setOf(OWNER_MIGUEL), exact = true),
            Golden("owner of Comet", expected = setOf(PATIENT_COMET), exact = true),
            // --- Multi-patient disambiguation ---
            Golden("West Nile Virus", expected = setOf(VACC_WEST_NILE), exact = true),
            Golden("Comet cough", expected = setOf(PATIENT_COMET, CONSULT_COUGH), exact = true),
            Golden(
                "Bella knee radiograph",
                expected = setOf(IMAGING_KNEE),
                forbidden = setOf(PATIENT_THUNDER, PATIENT_COMET),
            ),
            Golden(
                "Thunder navicular",
                expected = setOf(PATIENT_THUNDER, LAMENESS_NAVICULAR),
                forbidden = setOf(PATIENT_BELLA, PATIENT_COMET),
                exact = true,
            ),
            Golden(
                "Bella follicle",
                // OR retry: "bella*" hits only the patient row; "follicle*"
                // pulls both mares' ultrasounds plus the Heat event.
                expected = setOf(PATIENT_BELLA, US_BELLA_FOLLICLE, REPRO_HEAT, US_THUNDER_FOLLICLE),
                exact = true,
            ),
            Golden("follicle 22", expected = setOf(US_BELLA_FOLLICLE, REPRO_HEAT), exact = true),
            Golden("follicle 35", expected = setOf(US_THUNDER_FOLLICLE), exact = true),
            Golden("Eclipse", expected = setOf(REPRO_BREEDING, GESTATION_ACTIVE), exact = true),
            Golden("Breeding Eclipse fresh", expected = setOf(REPRO_BREEDING), exact = true),
            Golden("Heat", expected = setOf(REPRO_HEAT), exact = true),
            Golden("Icsi oocytes", expected = setOf(ICSI_COMET), exact = true),
            // --- Weight series ---
            Golden("512", expected = setOf(WEIGHT_512), exact = true),
            Golden("480", expected = setOf(WEIGHT_COMET), exact = true),
            // --- Nonsense queries expect empty ---
            Golden("zzqxjv", expected = emptySet(), exact = true),
            Golden("qqq www", expected = emptySet(), exact = true),
        )
    }

    @Test
    fun goldenRecallBaseline() {
        val failures = mutableListOf<String>()
        for (golden in goldenSet()) {
            val results = retrieve(golden.question)
            val keys = results.map { it.key() }.toSet()
            val missing = golden.expected - keys
            val leaked = keys intersect golden.forbidden
            val unexpected = if (golden.exact) keys - golden.expected else emptySet()
            if (missing.isNotEmpty() || leaked.isNotEmpty() || unexpected.isNotEmpty()) {
                failures +=
                    buildString {
                        appendLine("Q: \"${golden.question}\"")
                        if (missing.isNotEmpty()) appendLine("  missing: $missing")
                        if (leaked.isNotEmpty()) appendLine("  forbidden leaked: $leaked")
                        if (unexpected.isNotEmpty()) appendLine("  unexpected extra: $unexpected")
                        appendLine("  retrieved: $keys")
                    }
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} golden question(s) regressed:\n${failures.joinToString("\n")}")
    }

    /**
     * Locks the CURRENT default ordering (`ORDER BY p.name, i.date`, SQLite
     * ASC puts NULL dates first) for a tie-free query. Ranking changes must
     * update this expectation deliberately.
     */
    @Test
    fun currentDefaultOrderWilson() {
        val orderedKeys = retrieve("Wilson").map { it.key() }
        assertEquals(
            listOf(
                // Bella group (p.name ASC), i.date ascending:
                VACC_WEST_NILE, // 2026-04-12
                US_BELLA_FOLLICLE, // 2026-05-18
                DEWORM_FENBENDAZOLE, // 2026-05-20
                LAB_COGGINS, // 2026-05-21
                IMAGING_KNEE, // 2026-06-01
                // Comet group:
                CONSULT_COUGH, // 2026-04-08
                // Thunder group:
                DENTISTRY_FLOATING, // 2026-01-28
            ),
            orderedKeys,
            "default order changed - update this baseline deliberately with the ranking change",
        )
    }

    /** Weight trend for Thunder stays retrievable within its record type. */
    @Test
    fun weightSeriesForThunderRecall() {
        // Weight snippets carry only the kg value ("512.0"), so the query
        // targets the values themselves; the type filter scopes to weights.
        val weights =
            searchUseCase("5", from = null, to = null, recordTypes = listOf(RecordType.Weight.wireName))
                .sortedBy { it.date }
        assertEquals(listOf(WEIGHT_512, WEIGHT_525, WEIGHT_538), weights.map { it.key() })
        assertEquals(
            listOf(512.0, 525.5, 538.0),
            weights.map { it.snippet.trim().toDouble() },
            "weight series must keep its increasing trend visible to the model",
        )
    }

    /** Date bounds filter on the indexed record date. */
    @Test
    fun dateWindowFiltersColicRecords() {
        val february =
            searchUseCase("colic", from = LocalDate(2026, 2, 1), to = LocalDate(2026, 2, 28), recordTypes = null)
                .map { it.key() }
                .toSet()
        assertEquals(setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), february)

        val juneOnward = searchUseCase("colic", from = LocalDate(2026, 6, 1), to = null, recordTypes = null)
        assertTrue(juneOnward.isEmpty(), "no colic records exist after June 2026")
    }

    /** The seeded fixture covers every indexed row kind exactly once. */
    @Test
    fun fixtureCoversAllIndexedRows() {
        // 3 patients + 2 owners + 29 clinical/repro records.
        val expectedRows = 34
        assertEquals(expectedRows.toLong(), database.searchFtsQueries.countIndexRows().executeAsOne())
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun SearchResult.key(): String = "$recordType#$recordId"

    /**
     * Mirrors the production retrieval path in [GenerateRagResponseUseCase]:
     * filler-stripped AND query first, one broad OR retry when the AND query
     * zeroes out.
     *
     * The OR leg goes through [SearchRepositoryImpl] directly with an
     * FTS-safe expression from [AssistantPrompts.toFtsOrQuery]:
     * [SearchUseCase] star-joins every whitespace token, so an OR-joined
     * fallback becomes `a* AND OR* AND b*` and FTS5 rejects the reserved
     * operator carrying a suffix star (retrieval bug fixed in the llm lane).
     */
    private fun retrieve(question: String): List<SearchResult> =
        searchUseCase(AssistantPrompts.enrichQuery(question), from = null, to = null, recordTypes = null)
            .ifEmpty { searchRepo.search(AssistantPrompts.toFtsOrQuery(question), null, null, null) }
}
