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
        /** Mirrors GenerateRagResponseUseCase.WEAK_RESULT_THRESHOLD (private there). */
        const val WEAK_RESULT_THRESHOLD = 3

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

        // Expanded fixture (second seeding wave): near-duplicate names for
        // disambiguation coverage plus fresh vets so the locked Wilson/Costa/
        // House/Mendes/Smith expectations stay untouched.
        const val PATIENT_THUNDERSTORM = "PATIENT#4"
        const val PATIENT_BELINHA = "PATIENT#5"
        const val PATIENT_TROVOADA = "PATIENT#6"
        const val PATIENT_ISABELLA = "PATIENT#7"
        const val OWNER_ANA = "OWNER#3"
        const val OWNER_SOFIA = "OWNER#4"
        const val VACC_RABIES = "VACCINATION#104"
        const val VACC_EHV = "VACCINATION#105"
        const val VACC_EVA = "VACCINATION#106"
        const val DEWORM_MOXIDECTIN = "DEWORMING#203"
        const val DEWORM_PYRANTEL = "DEWORMING#204"
        const val FARRIER_TRIM_STORM = "FARRIER_VISIT#302"
        const val FARRIER_TRIM_TROV = "FARRIER_VISIT#303"
        const val GESTATION_FAILED = "GESTATION#403"
        const val US_STORM_TENDON = "ULTRASOUND#503"
        const val WEIGHT_495 = "WEIGHT#605"
        const val WEIGHT_410 = "WEIGHT#606"
        const val WEIGHT_380 = "WEIGHT#607"
        const val WEIGHT_290 = "WEIGHT#608"
        const val CONSULT_WIRE_CUT = "CONSULTATION#703"
        const val CONSULT_DERMATITIS = "CONSULTATION#704"
        const val CONSULT_CHOKE = "CONSULTATION#705"
        const val CONSULT_QUIDDING = "CONSULTATION#706"
        const val LAMENESS_SUSPENSORY = "LAMENESS#902"
        const val LAB_FECAL = "LAB_RESULT#1003"
        const val LAB_ACTH = "LAB_RESULT#1004"
        const val SUBSTANCE_XYLAZINE = "CONTROLLED_SUBSTANCE#1102"
        const val REPRO_PREG_CHECK = "REPRODUCTION_EVENT#1203"
        const val DENTISTRY_WAVE_MOUTH = "DENTISTRY#1402"
    }

    /** Medication ids are assigned by the save path; resolved during seeding. */
    private var medicationId: Long = 0L
    private var medication2Id: Long = 0L

    init {
        seedFixture()
    }

    private fun seedFixture() {
        seedOwnersAndPatients()
        seedThunderRecords()
        seedBellaRecords()
        seedCometRecords()
        seedThunderstormRecords()
        seedBelinhaRecords()
        seedTrovoadaRecords()
        seedIsabellaRecords()
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
        database.ownerQueries.insertWithId(
            id = 3L,
            name = "Ana Ferreira",
            email = "ana@example.com",
            phone = null,
            address = "Rua Azul 8",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.ownerQueries.insertWithId(
            id = 4L,
            name = "Sofia Marques",
            email = "sofia@example.com",
            phone = "+351916000111",
            address = "Monte Verde",
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
        // Near-duplicate of "Thunder" for prefix-disambiguation coverage.
        database.patientQueries.insertWithId(
            id = 4L,
            name = "Thunderstorm",
            species = "Equine",
            breed = "Standardbred",
            dateOfBirth = LocalDate(2018, 8, 1),
            gender = "Mare",
            microchipId = "MC777111",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Barn A",
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
        database.patientQueries.insertWithId(
            id = 5L,
            name = "Belinha",
            species = "Equine",
            breed = "Arabian",
            dateOfBirth = LocalDate(2021, 5, 20),
            gender = "Mare",
            microchipId = "MC222333",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Quinta do Sol",
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
            id = 6L,
            name = "Trovoada",
            species = "Equine",
            breed = "Sorraia",
            dateOfBirth = LocalDate(2016, 9, 30),
            gender = "Gelding",
            microchipId = "MC444555",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Herdade Nova",
            photoUri = null,
            notes = null,
            ownerId = 3L,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        // Contains "Bella" as a substring but NOT as a token prefix - FTS
        // prefix matching is anchored at the token start, so "bella*" must
        // not reach this row.
        database.patientQueries.insertWithId(
            id = 7L,
            name = "Isabella",
            species = "Equine",
            breed = "Welsh Pony",
            dateOfBirth = LocalDate(2019, 11, 10),
            gender = "Mare",
            microchipId = "MC888999",
            ueln = null,
            registrationNumber = null,
            stableLocation = "Barn C",
            photoUri = null,
            notes = null,
            ownerId = 4L,
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

    private fun seedThunderstormRecords() {
        database.vaccinationQueries.insertWithId(
            id = 104L,
            patientId = 4L,
            vaccineName = "Rabies",
            dateAdministered = LocalDate(2026, 3, 3),
            nextDueDate = LocalDate(2027, 3, 3),
            vetName = "Dr. Nunes",
            batchNumber = "RAB-2026-010",
            site = "Left hip",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dewormingQueries.insertWithId(
            id = 203L,
            patientId = 4L,
            product = "Moxidectin",
            dateAdministered = LocalDate(2026, 4, 10),
            nextDueDate = LocalDate(2026, 10, 10),
            dose = "400 ug/kg",
            vetName = "Dr. Nunes",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        seedWeight(605L, patientId = 4L, weightKg = 495.0, date = LocalDate(2026, 4, 10))
        database.farrierVisitQueries.insertWithId(
            id = 302L,
            patientId = 4L,
            date = LocalDate(2026, 4, 12),
            trimOrShoe = "Trim",
            shoeType = "Aluminum fronts",
            findings = "Hoof wall crack",
            nextDueDate = LocalDate(2026, 7, 12),
            farrier = "Rui Alves",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.lamenessQueries.insertWithId(
            id = 902L,
            patientId = 4L,
            date = LocalDate(2026, 5, 15),
            gradeAAEP = 2,
            limbLocation = "Left hindlimb",
            flexionTest = "Negative",
            diagnosis = "Suspensory desmitis",
            treatment = "Shockwave therapy",
            vetName = "Dr. Pinto",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.consultationQueries.insertWithId(
            id = 703L,
            patientId = 4L,
            date = LocalDate(2026, 5, 2),
            subjective = "Wire cut on flank",
            objective = "Superficial laceration",
            assessment = "Skin wound",
            plan = "Clean and suture",
            vetName = "Dr. Nunes",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dentistryQueries.insertWithId(
            id = 1402L,
            patientId = 4L,
            date = LocalDate(2026, 3, 15),
            findings = "Wave mouth",
            treatment = "Floating",
            nextDueDate = LocalDate(2026, 9, 15),
            vetName = "Dr. Almeida",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.ultrasoundQueries.insertWithId(
            id = 503L,
            patientId = 4L,
            date = LocalDate(2026, 6, 20),
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
            findings = "SDFT tendinitis left forelimb",
            imageUris = null,
            vetName = "Dr. Pinto",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        // Resolved (Failed) gestation: deliberately gets NO pregnancy
        // vocabulary, mirroring the production resolved-status rule.
        database.gestationQueries.insertWithId(
            id = 403L,
            patientId = 4L,
            breedingDate = LocalDate(2026, 5, 1),
            expectedDueDate = LocalDate(2026, 4, 5),
            gestationDays = 30,
            status = "Failed",
            fetalCount = 0,
            lastCheckDate = LocalDate(2026, 5, 31),
            notes = "Early loss day 30",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.substanceQueries.insertWithId(
            id = 1102L,
            patientId = 4L,
            drugName = "Xylazine",
            dose = "20",
            unit = "mg",
            route = "IV",
            administeredBy = "Dr. Pinto",
            witness = "Nurse Alves",
            date = LocalDate(2026, 5, 2),
            reason = "Standing sedation",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun seedBelinhaRecords() {
        database.vaccinationQueries.insertWithId(
            id = 105L,
            patientId = 5L,
            vaccineName = "Equine Herpes Virus",
            dateAdministered = LocalDate(2026, 2, 14),
            nextDueDate = LocalDate(2027, 2, 14),
            vetName = "Dr. Almeida",
            batchNumber = "EHV-2026-003",
            site = "Neck",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.dewormingQueries.insertWithId(
            id = 204L,
            patientId = 5L,
            product = "Pyrantel",
            dateAdministered = LocalDate(2026, 6, 25),
            nextDueDate = LocalDate(2026, 12, 25),
            dose = "10 mg/kg",
            vetName = "Dr. Nunes",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        seedWeight(606L, patientId = 5L, weightKg = 410.0, date = LocalDate(2026, 6, 25))
        database.consultationQueries.insertWithId(
            id = 704L,
            patientId = 5L,
            date = LocalDate(2026, 7, 5),
            subjective = "Itchy skin and hives",
            objective = "Urticarial bumps neck",
            assessment = "Allergic dermatitis",
            plan = "Antihistamine course",
            vetName = "Dr. Almeida",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.labResultQueries.insertWithId(
            id = 1003L,
            patientId = 5L,
            testType = "Fecal Egg Count",
            date = LocalDate(2026, 6, 25),
            results = "Strongyle eggs 250 epg",
            normalRange = "0-200 epg",
            vetName = "Dr. Nunes",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.reproductionQueries.insertWithId(
            id = 1203L,
            patientId = 5L,
            eventType = "Pregnancy Check",
            date = LocalDate(2026, 7, 15),
            details = "Single viable fetus 60 days",
            initialExamFindings = null,
            stallionName = null,
            breedingType = null,
            vetName = "Dr. Pinto",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun seedTrovoadaRecords() {
        database.vaccinationQueries.insertWithId(
            id = 106L,
            patientId = 6L,
            vaccineName = "Equine Viral Arteritis",
            dateAdministered = LocalDate(2026, 5, 20),
            nextDueDate = LocalDate(2027, 5, 20),
            vetName = "Dr. Pinto",
            batchNumber = "EVA-2026-001",
            site = "Neck",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.consultationQueries.insertWithId(
            id = 705L,
            patientId = 6L,
            date = LocalDate(2026, 4, 18),
            subjective = "Choke on grain overnight",
            objective = "Salivation, neck extended",
            assessment = "Esophageal obstruction",
            plan = "Smooth muscle relaxant",
            vetName = "Dr. Nunes",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.farrierVisitQueries.insertWithId(
            id = 303L,
            patientId = 6L,
            date = LocalDate(2026, 6, 30),
            trimOrShoe = "Trim",
            shoeType = "Barefoot",
            findings = "Hoof rings",
            nextDueDate = LocalDate(2026, 9, 30),
            farrier = "Rui Alves",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        seedWeight(607L, patientId = 6L, weightKg = 380.0, date = LocalDate(2026, 4, 18))
    }

    private fun seedIsabellaRecords() {
        database.consultationQueries.insertWithId(
            id = 706L,
            patientId = 7L,
            date = LocalDate(2026, 6, 12),
            subjective = "Dropping food while eating (quidding)",
            objective = "Sharp points molars, low body condition score",
            assessment = "Dental discomfort",
            plan = "Schedule dental exam",
            vetName = "Dr. Almeida",
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        database.labResultQueries.insertWithId(
            id = 1004L,
            patientId = 7L,
            testType = "ACTH",
            date = LocalDate(2026, 6, 12),
            results = "Elevated",
            normalRange = "Seasonal range",
            vetName = "Dr. Pinto",
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        seedWeight(608L, patientId = 7L, weightKg = 290.0, date = LocalDate(2026, 6, 12))
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
        medication2Id =
            saveMedication(
                Medication(
                    id = 0L,
                    patientId = 3L,
                    name = "Prednisolone",
                    dosage = "100 mg once daily",
                    route = "Oral",
                    frequency = "Once daily",
                    startDate = LocalDate(2026, 7, 10),
                    endDate = null,
                    prescribedBy = "Dr. Pinto",
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
            // "thunder*" prefix-matches BOTH Thunder and Thunderstorm patient
            // rows (near-duplicate added deliberately); FTS has no exact-term
            // mode through this path, so bare short names are inherently broad.
            Golden("Thunder", expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM), exact = true),
            // "tell"/"me"/"about" are filler words, so the OR retry reduces to
            // "thunder*" and hits only the two thunder* patient rows - the
            // conversational lead-in no longer leaks every "me..." record
            // (Mendes, Metronidazole). Previously locked WITH that leak;
            // tightened when the filler list gained the pronouns.
            Golden(
                "Tell me about Thunder",
                expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM),
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
            Golden("Detomidine sedation", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            // FLIPPED (weak-leg retry): the single AND hit no longer
            // suppresses the OR retry; booster*/tetanus* plus the vaccination
            // synonym group pull every vaccination row's generic vocabulary.
            Golden(
                "Tetanus booster",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            Golden("Fenbendazole", expected = setOf(DEWORM_FENBENDAZOLE), exact = true),
            Golden("Deslorelin", expected = setOf(REPRO_MEDICATION_DESLORELIN), exact = true),
            // FLIPPED (weak-leg retry): colic* unions the Detomidine row
            // ("Colic sedation") next to the consultation.
            Golden("colic surgery", expected = setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), exact = true),
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
            Golden(
                "Is Thunder in foal?",
                // AND leg zeroes out (patient names never appear in record
                // text); the OR retry unions thunder* patient rows with the
                // foal/gestation vocabulary carried by the ACTIVE gestation.
                expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM, GESTATION_ACTIVE),
                exact = true,
            ),
            Golden("pregnant", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("gestation", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("Completed", expected = setOf(GESTATION_COMPLETED), exact = true),
            Golden("filly", expected = setOf(GESTATION_COMPLETED), exact = true),
            // Current gaps pinned as empty: synonym expansion must flip these deliberately.
            // FLIPPED (deliberate recall gains):
            // - "vaccination": vaccination rows now index generic vaccination
            //   vocabulary ("vaccination vaccine booster shot"), so the AND
            //   query itself retrieves every vaccination record instead of
            //   zeroing out on raw field values (vaccine name/batch/site).
            Golden(
                "vaccination",
                // Every vaccination row carries the injected vocabulary, so
                // this grows with the fixture by design (6 rows seeded).
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            // FLIPPED: "shod" shares no token with the indexed farrier text;
            // the hoof-care synonym group (shod/shoeing/shoes/trim/farrier)
            // bridges it through the OR retry to every farrier visit.
            Golden(
                "shod",
                expected = setOf(FARRIER_SHOEING, FARRIER_TRIM_STORM, FARRIER_TRIM_TROV),
                exact = true,
            ),
            // FLIPPED: embryo-transfer rows now index "embryo transfer flush
            // donor recipient" vocabulary, so the natural phrase retrieves the
            // ET record whose raw fields (a bare count and recipient mare
            // names) never contained those words.
            Golden("embryo transfer", expected = setOf(ET_BELLA), exact = true),
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
            // FLIPPED (weak-leg retry): virus* unions the EHV row ("Equine
            // Herpes Virus") - honest shared-token breadth.
            Golden("West Nile Virus", expected = setOf(VACC_WEST_NILE, VACC_EHV), exact = true),
            Golden("Comet cough", expected = setOf(PATIENT_COMET, CONSULT_COUGH), exact = true),
            Golden(
                "Bella knee radiograph",
                expected = setOf(IMAGING_KNEE),
                forbidden = setOf(PATIENT_THUNDER, PATIENT_COMET),
            ),
            Golden(
                "Thunder navicular",
                // OR retry: thunder* now hits both thunder* patient rows.
                expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM, LAMENESS_NAVICULAR),
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
            // FLIPPED (weak-leg retry): follicle* unions both mares'
            // ultrasounds with the Heat event.
            Golden("follicle 22", expected = setOf(US_BELLA_FOLLICLE, REPRO_HEAT, US_THUNDER_FOLLICLE), exact = true),
            // FLIPPED (weak-leg retry): follicle* unions the 22 mm rows;
            // numeric "35*" also leaks the two +351 phone tokens (same
            // numeric-prefix breadth class as the pinned "grade 3").
            Golden(
                "follicle 35",
                expected = setOf(US_THUNDER_FOLLICLE, US_BELLA_FOLLICLE, REPRO_HEAT, OWNER_DANIELA, OWNER_SOFIA),
                exact = true,
            ),
            Golden("Eclipse", expected = setOf(REPRO_BREEDING, GESTATION_ACTIVE), exact = true),
            // FLIPPED (weak-leg retry): eclipse* unions the active gestation
            // whose notes name stallion Eclipse.
            Golden("Breeding Eclipse fresh", expected = setOf(REPRO_BREEDING, GESTATION_ACTIVE), exact = true),
            Golden("Heat", expected = setOf(REPRO_HEAT), exact = true),
            Golden("Icsi oocytes", expected = setOf(ICSI_COMET), exact = true),
            // --- Weight series ---
            Golden("512", expected = setOf(WEIGHT_512), exact = true),
            Golden("480", expected = setOf(WEIGHT_COMET), exact = true),
            // --- Portuguese questions (language-mirroring recall baseline) ---
            // "égua" has no indexed counterpart (the fixture's vocabulary is
            // English), but the proper noun recovers the mare via the OR
            // retry - expected behavior for mixed-language questions.
            Golden("Égua Lusitano", expected = setOf(PATIENT_BELLA), exact = true),
            // PT clinical term "cólica" folds to "colica", which does NOT
            // prefix-match the indexed "colic"; only the thunder* patient
            // rows hit (via the OR retry). Documents the known PT-vocabulary
            // gap: the assistant still finds the horses, but not the colic
            // consultation, from PT wording.
            Golden(
                "Thunder teve cólica aguda?",
                expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM),
                exact = true,
            ),
            // Fully-PT clinical question with no proper noun: every token
            // misses the English-only index ("vacinas*" ≠ "vaccine*"), so the
            // retrieval is empty and the assistant answers with its honest
            // no-results fallback. Pinned gap - flips when PT synonyms land.
            Golden("Quantas vacinas foram administradas?", expected = emptySet(), exact = true),
            // --- Punctuation / hyphen robustness ---
            // Hyphenated input tokenizes exactly like the spaced content in
            // the record ("Steel full set"), so the AND leg matches directly.
            // FLIPPED (weak-leg retry): the hoof-care synonym group fires on
            // "shoeing" and unions both Trim rows through farrier*.
            Golden(
                "Steel full-set shoeing",
                expected = setOf(FARRIER_SHOEING, FARRIER_TRIM_STORM, FARRIER_TRIM_TROV),
                exact = true,
            ),
            // Possessive apostrophe is stripped by AssistantPrompts.clean()
            // ("Thunder's" -> "Thunders"); the name token misses (record text
            // never carries patient names), so the broad OR retry runs and
            // "booster*"/"vaccine*"/"shot*" - part of the generic vaccination
            // vocabulary indexed on EVERY vaccination row - pulls all six.
            // Documents both the apostrophe fix and the shared-vocabulary
            // recall breadth (grows with the fixture by design).
            Golden(
                "Thunder's tetanus booster",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                        // "Thunders*" prefix-matches the "thunderstorm" token.
                        PATIENT_THUNDERSTORM,
                    ),
                forbidden = setOf(PATIENT_THUNDER),
                exact = true,
            ),
            // =============================================================
            // Expanded baseline (wave 2): type-name vocabulary, record-type
            // breadth, clinical phrasings, dates, gaps, disambiguation,
            // PT, typos, analysis intent, punctuation, near-duplicates.
            // =============================================================
            // --- Type-name vocabulary: record-type words are NOT indexed ---
            // Except where vocabulary is injected (vaccination/embryo
            // transfer/pregnancy), the type label itself never appears in
            // searchableText - rows carry raw field values only.
            Golden("lameness", expected = emptySet(), exact = true),
            Golden("ultrasound", expected = emptySet(), exact = true),
            Golden("dentistry", expected = emptySet(), exact = true),
            Golden("deworming", expected = emptySet(), exact = true),
            Golden("imaging", expected = emptySet(), exact = true),
            // Medication save path indexes name+dosage only.
            Golden("medication", expected = emptySet(), exact = true),
            // Contrast: "surgery" DOES hit - the word occurs in the colic
            // consultation's plan text ("Referral for colic surgery").
            Golden("surgery", expected = setOf(CONSULT_COLIC), exact = true),
            Golden("controlled substance", expected = emptySet(), exact = true),
            // No reminder rows exist in the search index at all.
            Golden("custom reminders", expected = emptySet(), exact = true),
            // Gender is stored on Patient but never indexed.
            Golden("gelding", expected = emptySet(), exact = true),
            // Only microchip VALUES (MC...) are indexed, not the field label.
            Golden("microchip", expected = emptySet(), exact = true),
            Golden("ueln", expected = emptySet(), exact = true),
            // --- Record-type breadth: lameness ---
            // FLIPPED (field labels + weak-leg retry): "flexion" is now
            // indexed as a field label, so flexion* unions both lameness
            // rows; positive* keeps Navicular anchored.
            Golden("flexion test positive", expected = setOf(LAMENESS_NAVICULAR, LAMENESS_SUSPENSORY), exact = true),
            // FLIPPED (field labels + weak-leg retry): flexion* adds the
            // Positive navicular row to the Negative union.
            Golden(
                "flexion negative",
                expected = setOf(LAMENESS_SUSPENSORY, LAB_COGGINS, LAMENESS_NAVICULAR),
                exact = true,
            ),
            Golden("suspensory desmitis", expected = setOf(LAMENESS_SUSPENSORY), exact = true),
            Golden("shockwave therapy", expected = setOf(LAMENESS_SUSPENSORY), exact = true),
            // "grade" is now an indexed field label (v8); the OR retry unions
            // every row holding a 3* token - including weight "380" and the
            // phone numbers starting +351 - and grade* adds the grade-2
            // suspensory row.
            Golden(
                "grade 3",
                expected =
                    setOf(
                        LAMENESS_NAVICULAR,
                        LAMENESS_SUSPENSORY,
                        US_THUNDER_FOLLICLE,
                        GESTATION_FAILED,
                        WEIGHT_380,
                        OWNER_DANIELA,
                        // Sofia's +351... phone token starts with "3"; Ana has no digit.
                        OWNER_SOFIA,
                    ),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): forelimb* unions both forelimb rows
            // (navicular Right, tendon ultrasound Left) plus right* reaching
            // Bella's "right ovary" ultrasound.
            Golden(
                "right forelimb",
                expected = setOf(LAMENESS_NAVICULAR, US_STORM_TENDON, US_BELLA_FOLLICLE),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): left*/hindlimb* union every row with
            // a left- token (tendon ultrasound, left ovary, Left hip and
            // Left shoulder vaccination sites).
            Golden(
                "left hindlimb",
                expected = setOf(LAMENESS_SUSPENSORY, US_STORM_TENDON, US_THUNDER_FOLLICLE, VACC_RABIES, VACC_TETANUS),
                exact = true,
            ),
            // Cross-type token: "forelimb" appears in the lameness row AND in
            // the tendon ultrasound findings.
            Golden("forelimb", expected = setOf(LAMENESS_NAVICULAR, US_STORM_TENDON), exact = true),
            Golden("bute", expected = setOf(LAMENESS_NAVICULAR), exact = true),
            // --- Record-type breadth: surgery / imaging ---
            Golden("chip fragment", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            Golden("successful", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            Golden("general anesthesia", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            // FLIPPED (weak-leg retry): stall* unions the active gestation
            // ("AI with stallion Eclipse" - stallion carries the stall-
            // prefix) and rest* the cough consult and navicular row.
            Golden(
                "stall rest",
                expected = setOf(SURGERY_ARTHROSCOPY, GESTATION_ACTIVE, CONSULT_COUGH, LAMENESS_NAVICULAR),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): numeric 6* leaks the "60 days" fetus
            // token on the pregnancy check (same numeric-prefix breadth class
            // as the pinned "grade 3").
            Golden("6 weeks", expected = setOf(SURGERY_ARTHROSCOPY, REPRO_PREG_CHECK), exact = true),
            Golden("phenylbutazone", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            // "recovery" exists only as a FIELD NAME (recoveryNotes); its
            // indexed VALUE is "Stall rest 6 weeks". Field labels are never
            // indexed, so the word cannot match.
            Golden("arthroscopy recovery", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            // FLIPPED (field labels, v8): "surgeon" is indexed on the surgery
            // row, so "who was the surgeon" style queries hit.
            Golden("surgeon", expected = setOf(SURGERY_ARTHROSCOPY), exact = true),
            // Null column -> its label and value are both absent.
            Golden("complications", expected = emptySet(), exact = true),
            // OR retry unions rows across types sharing one token each.
            Golden("stifle surgery", expected = setOf(SURGERY_ARTHROSCOPY, CONSULT_COLIC), exact = true),
            Golden("radiograph knee", expected = setOf(IMAGING_KNEE), exact = true),
            // --- Record-type breadth: controlled substances ---
            Golden("xylazine", expected = setOf(SUBSTANCE_XYLAZINE), exact = true),
            Golden("detomidine", expected = setOf(SUBSTANCE_DETOMIDINE), exact = true),
            Golden("sedation", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            // FLIPPED (weak-leg retry): the single AND hit no longer
            // suppresses the OR retry; sedation* unions Detomidine's "Colic
            // sedation" next to Xylazine's "Standing sedation".
            Golden("standing sedation", expected = setOf(SUBSTANCE_XYLAZINE, SUBSTANCE_DETOMIDINE), exact = true),
            // FLIPPED (field labels, v8): "witness" is indexed on controlled
            // substance rows for "who witnessed" regulatory queries.
            Golden("witness", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            Golden("nurse", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            // FLIPPED (weak-leg retry): nurse* unions the second substance
            // row ("Nurse Alves") with Silva's.
            Golden("Nurse Silva", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            // FLIPPED (short-prefix guard): "IV" is 2 letters, so it now
            // matches EXACTLY - the route value on both substance rows - and
            // no longer prefix-leaks onto "Ivermectin".
            Golden(
                "IV",
                expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE),
                exact = true,
            ),
            // --- Record-type breadth: labs ---
            Golden("ELISA", expected = setOf(LAB_COGGINS), exact = true),
            Golden("fecal egg count", expected = setOf(LAB_FECAL), exact = true),
            Golden("strongyle", expected = setOf(LAB_FECAL), exact = true),
            Golden("ACTH", expected = setOf(LAB_ACTH), exact = true),
            // --- Record-type breadth: dentistry ---
            // FLIPPED (weak-leg retry): points* unions the quidding consult
            // ("Sharp points molars") with the enamel row.
            Golden("enamel points", expected = setOf(DENTISTRY_FLOATING, CONSULT_QUIDDING), exact = true),
            Golden("wave mouth", expected = setOf(DENTISTRY_WAVE_MOUTH), exact = true),
            // "teeth" never appears (indexed value is "Floating"); the OR
            // retry still recovers both floating rows via float*.
            Golden(
                "floating teeth",
                expected = setOf(DENTISTRY_FLOATING, DENTISTRY_WAVE_MOUTH),
                exact = true,
            ),
            Golden("quidding", expected = setOf(CONSULT_QUIDDING), exact = true),
            Golden("molars", expected = setOf(CONSULT_QUIDDING), exact = true),
            // --- Record-type breadth: farrier ---
            // FLIPPED (weak-leg retry): the two Trim AND-hits no longer
            // suppress the synonym retry; farrier* unions the Shoeing row.
            Golden("trim", expected = setOf(FARRIER_SHOEING, FARRIER_TRIM_STORM, FARRIER_TRIM_TROV), exact = true),
            Golden("farrier", expected = setOf(FARRIER_SHOEING, FARRIER_TRIM_STORM, FARRIER_TRIM_TROV), exact = true),
            // FLIPPED (weak-leg retry): alves* unions the Xylazine row whose
            // witness is "Nurse Alves".
            Golden("Rui Alves", expected = setOf(FARRIER_TRIM_STORM, FARRIER_TRIM_TROV, SUBSTANCE_XYLAZINE), exact = true),
            Golden("aluminum", expected = setOf(FARRIER_TRIM_STORM), exact = true),
            Golden("barefoot", expected = setOf(FARRIER_TRIM_TROV), exact = true),
            // FLIPPED (weak-leg retry): hoof* unions the wall-crack Trim row.
            Golden("hoof rings", expected = setOf(FARRIER_TRIM_TROV, FARRIER_TRIM_STORM), exact = true),
            Golden("hoof", expected = setOf(FARRIER_TRIM_STORM, FARRIER_TRIM_TROV), exact = true),
            // "abscess" is absent from every seeded row (true vocabulary
            // gap in the fixture, not an indexing bug); hoof* carries the
            // farrier rows through the OR retry.
            Golden(
                "hoof abscess",
                expected = setOf(FARRIER_TRIM_STORM, FARRIER_TRIM_TROV),
                exact = true,
            ),
            // "shoes" misses leg 1; the synonym group fires in the OR leg.
            Golden(
                "shoes",
                expected = setOf(FARRIER_SHOEING, FARRIER_TRIM_STORM, FARRIER_TRIM_TROV),
                exact = true,
            ),
            // --- Record-type breadth: deworming / medication ---
            Golden("moxidectin", expected = setOf(DEWORM_MOXIDECTIN), exact = true),
            Golden("pyrantel", expected = setOf(DEWORM_PYRANTEL), exact = true),
            Golden("prednisolone", expected = setOf("MEDICATION#$medication2Id"), exact = true),
            Golden("ug/kg", expected = setOf(DEWORM_IVERMECTIN, DEWORM_MOXIDECTIN), exact = true),
            Golden("mg/kg", expected = setOf(DEWORM_FENBENDAZOLE, DEWORM_PYRANTEL), exact = true),
            // "dose" is a field label - only dose VALUES are indexed.
            Golden("Ivermectin dose", expected = setOf(DEWORM_IVERMECTIN), exact = true),
            // --- Record-type breadth: vaccinations by name/batch/site ---
            Golden("Influenza", expected = setOf(VACC_INFLUENZA), exact = true),
            Golden("rabies", expected = setOf(VACC_RABIES), exact = true),
            Golden("herpes", expected = setOf(VACC_EHV), exact = true),
            Golden("EHV", expected = setOf(VACC_EHV), exact = true),
            Golden("arteritis", expected = setOf(VACC_EVA), exact = true),
            Golden("EVA", expected = setOf(VACC_EVA), exact = true),
            Golden("WNV", expected = setOf(VACC_WEST_NILE), exact = true),
            // "flu*" also prefix-matches "flush" from the embryo-transfer vocabulary.
            Golden("FLU", expected = setOf(VACC_INFLUENZA, ET_BELLA), exact = true),
            Golden("batch RAB-2026-010", expected = setOf(VACC_RABIES), exact = true),
            Golden("neck", expected = setOf(VACC_INFLUENZA, VACC_WEST_NILE, VACC_EHV, VACC_EVA, CONSULT_DERMATITIS, CONSULT_CHOKE), exact = true),
            // FLIPPED (plural folding + weak-leg retry): "vaccinations"
            // singularizes into the vaccination synonym group, whose
            // expansions pull every vaccination row's generic vocabulary.
            Golden(
                "Neck site vaccinations",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                        CONSULT_DERMATITIS,
                        CONSULT_CHOKE,
                    ),
                exact = true,
            ),
            Golden("booster", expected = setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_WEST_NILE, VACC_RABIES, VACC_EHV, VACC_EVA), exact = true),
            Golden("shot", expected = setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_WEST_NILE, VACC_RABIES, VACC_EHV, VACC_EVA), exact = true),
            Golden("vaccine", expected = setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_WEST_NILE, VACC_RABIES, VACC_EHV, VACC_EVA), exact = true),
            // --- Multi-word clinical phrasings ---
            // FLIPPED (weak-leg retry): booster* + the vaccination synonym
            // group pull all six vaccination rows.
            Golden(
                "West Nile booster",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): shot* + the vaccination synonym group
            // pull all six vaccination rows.
            Golden(
                "tetanus shot",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): vaccine*/flu* + the vaccination group
            // pull all six vaccination rows; flu* keeps the ET flush leak.
            Golden(
                "flu vaccine",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                        ET_BELLA,
                    ),
                exact = true,
            ),
            Golden("dry cough", expected = setOf(CONSULT_COUGH), exact = true),
            Golden("RAO cough", expected = setOf(CONSULT_COUGH), exact = true),
            Golden("navicular", expected = setOf(LAMENESS_NAVICULAR), exact = true),
            Golden("colic", expected = setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), exact = true),
            // Reverse synonym direction: "abdominal pain" triggers the colic
            // group in the OR leg and reaches the English-indexed records.
            Golden("abdominal pain", expected = setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), exact = true),
            // Half of a multi-word synonym member does NOT trigger the group.
            Golden("pain", expected = emptySet(), exact = true),
            Golden("colic surgery recovery", expected = setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), exact = true),
            Golden("pregnancy check result", expected = setOf(REPRO_PREG_CHECK), exact = true),
            // FLIPPED (weak-leg retry): skin* unions the dermatitis consult
            // ("Itchy skin and hives").
            Golden("skin wound", expected = setOf(CONSULT_WIRE_CUT, CONSULT_DERMATITIS), exact = true),
            Golden("wire cut", expected = setOf(CONSULT_WIRE_CUT), exact = true),
            Golden("laceration", expected = setOf(CONSULT_WIRE_CUT), exact = true),
            Golden("suture", expected = setOf(CONSULT_WIRE_CUT), exact = true),
            Golden("choke", expected = setOf(CONSULT_CHOKE), exact = true),
            Golden("esophageal obstruction", expected = setOf(CONSULT_CHOKE), exact = true),
            Golden("dermatitis", expected = setOf(CONSULT_DERMATITIS), exact = true),
            Golden("hives", expected = setOf(CONSULT_DERMATITIS), exact = true),
            Golden("antihistamine", expected = setOf(CONSULT_DERMATITIS), exact = true),
            Golden("poor appetite", expected = emptySet(), exact = true),
            Golden("body condition", expected = setOf(CONSULT_QUIDDING), exact = true),
            // FLIPPED (tendon synonym group): "tendon" now triggers the
            // tendon/tendinitis morphology bridge, so the OR retry reaches
            // the tendinitis ultrasound without a stemmer.
            Golden("tendon injury", expected = setOf(US_STORM_TENDON), exact = true),
            Golden("tendinitis", expected = setOf(US_STORM_TENDON), exact = true),
            Golden("SDFT", expected = setOf(US_STORM_TENDON), exact = true),
            Golden("fetus", expected = setOf(REPRO_PREG_CHECK), exact = true),
            Golden("viable", expected = setOf(REPRO_PREG_CHECK), exact = true),
            // --- Reproduction phrasings ---
            Golden("Pregnancy Check", expected = setOf(REPRO_PREG_CHECK), exact = true),
            // "mare*" prefix-matches "Recipient mares" on the ET row.
            Golden("pregnant mare", expected = setOf(GESTATION_ACTIVE, ET_BELLA), exact = true),
            // "in" is filler, so this reduces to foal* which hits the active
            // gestation's injected vocabulary directly on leg 1.
            Golden("in foal", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("foaling", expected = setOf(GESTATION_ACTIVE), exact = true),
            // "bred" zeroes leg 1; the pregnancy synonym group fires in the
            // OR leg and recovers the active gestation.
            Golden("bred", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("failed", expected = setOf(GESTATION_FAILED), exact = true),
            Golden("active", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("Live filly born", expected = setOf(GESTATION_COMPLETED), exact = true),
            // FLIPPED (weak-leg retry): eclipse* unions the breeding event
            // whose stallion is Eclipse.
            Golden("Eclipse stallion", expected = setOf(GESTATION_ACTIVE, REPRO_BREEDING), exact = true),
            Golden("fresh cooled semen", expected = setOf(REPRO_BREEDING), exact = true),
            // FLIPPED (weak-leg retry): the exact "AI" token also matches the
            // active gestation's notes ("AI with stallion Eclipse").
            Golden("AI breeding", expected = setOf(REPRO_BREEDING, GESTATION_ACTIVE), exact = true),
            Golden("expected foaling date", expected = setOf(GESTATION_ACTIVE), exact = true),
            Golden("Recipient mares", expected = setOf(ET_BELLA), exact = true),
            Golden("flush", expected = setOf(ET_BELLA), exact = true),
            Golden("ovulation induction", expected = setOf(REPRO_MEDICATION_DESLORELIN), exact = true),
            // Numeric tokenization: "1.8 mg" splits into tokens 1 and 8, so
            // "8*" also reaches the Deslorelin dosage alongside the ICSI row.
            // ...and Ana's street address "Rua Azul 8".
            Golden("8 follicles", expected = setOf(ICSI_COMET, REPRO_MEDICATION_DESLORELIN, OWNER_ANA), exact = true),
            Golden("oocytes", expected = setOf(ICSI_COMET), exact = true),
            // --- Date-window questions (no temporal NLP in retrieval) ---
            // The harness passes from/to = null; time words are just tokens.
            // "colic*" carries the February records through the OR retry;
            // "this"/"year" match nothing.
            Golden("colic this year", expected = setOf(CONSULT_COLIC, SUBSTANCE_DETOMIDINE), exact = true),
            Golden("rabies last spring", expected = setOf(VACC_RABIES), exact = true),
            // FLIPPED (plural folding): "vaccines" singularizes into the
            // vaccination synonym group, whose expansions hit the generic
            // vocabulary on every vaccination row. Month names stay unindexed.
            Golden(
                "vaccines in January",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            Golden("January", expected = emptySet(), exact = true),
            // Years surface only through batch-number text.
            Golden(
                "2026",
                expected = setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_WEST_NILE, VACC_RABIES, VACC_EHV, VACC_EVA),
                exact = true,
            ),
            // --- Negation / true-absence gaps ---
            // Comet has no ultrasound row AND "ultrasound" is unindexed;
            // comet* anchors the patient row so the assistant can say so.
            Golden("ultrasound for Comet", expected = setOf(PATIENT_COMET), exact = true),
            // True absence: both colic records belong to Thunder; the OR
            // retry leaks them next to Comet's patient row (patient scoping
            // is NOT applied inside the fallback leg).
            Golden(
                "Colic for Comet",
                expected = setOf(PATIENT_COMET, CONSULT_COLIC, SUBSTANCE_DETOMIDINE),
                exact = true,
            ),
            // Cross-patient leak via OR retry: coggins* (Bella's lab) unions
            // with thunder* patient rows.
            Golden(
                "Coggins for Thunder",
                expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM, LAB_COGGINS),
                exact = true,
            ),
            // Comet is a gelding, but gender is unindexed and the word
            // "castration" appears nowhere - honest empty retrieval.
            Golden("castration", expected = emptySet(), exact = true),
            Golden("insurance", expected = emptySet(), exact = true),
            Golden("passport", expected = emptySet(), exact = true),
            // --- Cross-patient / cross-record disambiguation ---
            Golden("Fenbendazole Bella", expected = setOf(PATIENT_BELLA, DEWORM_FENBENDAZOLE), exact = true),
            Golden("Moxidectin Pyrantel", expected = setOf(DEWORM_MOXIDECTIN, DEWORM_PYRANTEL), exact = true),
            // Stable location anchors patient rows directly.
            Golden("Barn A horses", expected = setOf(PATIENT_THUNDER, PATIENT_COMET, PATIENT_THUNDERSTORM, PATIENT_ISABELLA), exact = true),
            // The stable letter suffix does NOT discriminate: leg 1 zeroes
            // (single-letter "b*"/"c*" prefixes match nothing here) and the
            // OR retry unions every barn* patient row.
            // FLIPPED (short-prefix guard): "B" is 1 letter, so it now matches
            // EXACTLY - surfacing the standalone "B" token in "Recipient mares
            // A and B" - while barn* still carries the patient rows.
            Golden(
                "Barn B",
                expected = setOf(PATIENT_THUNDER, PATIENT_COMET, PATIENT_THUNDERSTORM, PATIENT_ISABELLA, ET_BELLA),
                exact = true,
            ),
            // FLIPPED (weak-leg retry): the two Barn-C AND-hits no longer
            // suppress the retry; barn* unions every Barn-A patient row too.
            Golden("Barn C", expected = setOf(PATIENT_COMET, PATIENT_ISABELLA, PATIENT_THUNDER, PATIENT_THUNDERSTORM), exact = true),
            // FLIPPED (short-prefix guard + weak-leg retry): "do" now matches
            // EXACTLY instead of do*-exploding, and the single AND hit no
            // longer suppresses the retry - Belinha's "Quinta do Sol" joins
            // through its shared exact "do" token.
            Golden("Quinta do Vale", expected = setOf(PATIENT_BELLA, PATIENT_BELINHA), exact = true),
            Golden("Quinta do Sol", expected = setOf(PATIENT_BELINHA, PATIENT_BELLA), exact = true),
            // FLIPPED (weak-leg retry): herdade* unions Miguel's owner row
            // ("Herdade Boa Esperanca").
            Golden("Herdade Nova", expected = setOf(PATIENT_TROVOADA, OWNER_MIGUEL), exact = true),
            // FLIPPED (weak-leg retry): herdade* unions Trovoada's patient row
            // ("Herdade Nova").
            Golden("Herdade Boa Esperanca", expected = setOf(OWNER_MIGUEL, PATIENT_TROVOADA), exact = true),
            // --- Owner coverage beyond Daniela ---
            Golden("Ana Ferreira", expected = setOf(OWNER_ANA), exact = true),
            Golden("Sofia Marques", expected = setOf(OWNER_SOFIA), exact = true),
            Golden("ana@example.com", expected = setOf(OWNER_ANA), exact = true),
            Golden("Monte Verde", expected = setOf(OWNER_SOFIA), exact = true),
            Golden("Daniela", expected = setOf(OWNER_DANIELA), exact = true),
            // Surname ambiguity: Costa is BOTH the owner's name and Dr.
            // Costa's vet attribution on four repro records.
            Golden(
                "Costa",
                expected = setOf(OWNER_DANIELA, REPRO_BREEDING, REPRO_HEAT, REPRO_MEDICATION_DESLORELIN, ET_BELLA),
                exact = true,
            ),
            // Ownership is not indexed on either side of the relation; only
            // the patient-name anchor recovers anything.
            Golden("owner of Belinha", expected = setOf(PATIENT_BELINHA), exact = true),
            // --- Vet attribution breadth (new vets, locked old ones) ---
            Golden("Dr. Nunes", expected = setOf(VACC_RABIES, DEWORM_MOXIDECTIN, DEWORM_PYRANTEL, CONSULT_WIRE_CUT, CONSULT_CHOKE, LAB_FECAL), exact = true),
            Golden("Dr. Pinto", expected = setOf(LAMENESS_SUSPENSORY, US_STORM_TENDON, SUBSTANCE_XYLAZINE, REPRO_PREG_CHECK, LAB_ACTH, VACC_EVA), exact = true),
            Golden("Dr. Almeida", expected = setOf(VACC_EHV, CONSULT_DERMATITIS, DENTISTRY_WAVE_MOUTH, CONSULT_QUIDDING), exact = true),
            Golden("Mendes", expected = setOf(SURGERY_ARTHROSCOPY, ICSI_COMET), exact = true),
            // --- Portuguese (proper-noun anchored via OR retry) ---
            // "tem*" leaks onto "temperature" in Comet's SOAP text.
            Golden("Quantas vacinas tem a Thunder?", expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM, CONSULT_COUGH), exact = true),
            // FLIPPED (short-prefix guard): "da" is 2 letters, so it now
            // matches EXACTLY (no indexed standalone "da" token) instead of
            // exploding onto Daniela/daily/days/"day 30" - only the proper
            // noun survives.
            Golden(
                "Cólica da Trovoada",
                expected = setOf(PATIENT_TROVOADA),
                exact = true,
            ),
            // Diacritic folding gives "gestacao*", which misses "gestation";
            // only the proper noun survives.
            Golden("Trovoada gestação", expected = setOf(PATIENT_TROVOADA), exact = true),
            Golden("égua sorraia", expected = setOf(PATIENT_TROVOADA), exact = true),
            Golden("Belinha peso", expected = setOf(PATIENT_BELINHA), exact = true),
            Golden("Isabella vacina", expected = setOf(PATIENT_ISABELLA), exact = true),
            // Bare PT clinical term with no anchor: fully empty.
            Golden("cólica", expected = emptySet(), exact = true),
            // --- Typos / truncation robustness ---
            // Truncation IS covered by the prefix star...
            Golden("Thun", expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM), exact = true),
            Golden("Thundersto", expected = setOf(PATIENT_THUNDERSTORM), exact = true),
            Golden("Navicula", expected = setOf(LAMENESS_NAVICULAR), exact = true),
            Golden("MC123", expected = setOf(PATIENT_THUNDER), exact = true),
            // ...but internal typos are NOT: "thundr" is not a prefix of
            // "thunder", so the star cannot help. Honest empty retrieval.
            Golden("Thundr", expected = emptySet(), exact = true),
            Golden("Bellna", expected = emptySet(), exact = true),
            Golden("coggis", expected = emptySet(), exact = true),
            Golden("Detomidne", expected = emptySet(), exact = true),
            // PT drug spelling breaks the EN prefix.
            Golden("Ivermectina", expected = emptySet(), exact = true),
            // --- Analysis-intent phrasings (retrieval returns record sets;
            // summaries are computed separately by the RAG pipeline) ---
            // FLIPPED (plural folding): "vaccinations" singularizes into the
            // vaccination synonym group, so the OR retry now recovers all six
            // vaccination rows alongside the thunder* patient anchors.
            Golden(
                "How many vaccinations did Thunder have?",
                expected =
                    setOf(
                        PATIENT_THUNDER,
                        PATIENT_THUNDERSTORM,
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            // Weight rows index bare kg values, so "weight" itself never
            // matches - only the name anchor survives.
            Golden("average weight of Thunder", expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM), exact = true),
            Golden("how much does Comet weigh", expected = setOf(PATIENT_COMET), exact = true),
            // FLIPPED (plural folding): "vaccines" singularizes into the
            // vaccination synonym group and recovers every vaccination row.
            Golden(
                "How many vaccines this year?",
                expected =
                    setOf(
                        VACC_INFLUENZA,
                        VACC_TETANUS,
                        VACC_WEST_NILE,
                        VACC_RABIES,
                        VACC_EHV,
                        VACC_EVA,
                    ),
                exact = true,
            ),
            // "count*" leaks onto the Fecal Egg Count test type.
            Golden("count weight entries Thunderstorm", expected = setOf(PATIENT_THUNDERSTORM, LAB_FECAL), exact = true),
            Golden("How many foals does Daniela have?", expected = setOf(OWNER_DANIELA), exact = true),
            // --- Punctuation / case variants of passing questions ---
            Golden("IVERMECTIN", expected = setOf(DEWORM_IVERMECTIN), exact = true),
            Golden("coggins!", expected = setOf(LAB_COGGINS), exact = true),
            Golden("West-Nile-Virus", expected = setOf(VACC_WEST_NILE), exact = true),
            // FLIPPED (weak-leg retry): sedation* unions the Xylazine row.
            Golden("detomidine, sedation", expected = setOf(SUBSTANCE_DETOMIDINE, SUBSTANCE_XYLAZINE), exact = true),
            // Repository sanitizer splits "(rao)" on non-alphanumerics.
            Golden("(RAO)", expected = setOf(CONSULT_COUGH), exact = true),
            Golden(
                "THUNDER'S TETANUS BOOSTER",
                expected = setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_WEST_NILE, VACC_RABIES, VACC_EHV, VACC_EVA, PATIENT_THUNDERSTORM),
                exact = true,
            ),
            Golden("steel full set", expected = setOf(FARRIER_SHOEING), exact = true),
            Golden("   Comet   ", expected = setOf(PATIENT_COMET), exact = true),
            Golden("Quarter-horse", expected = setOf(PATIENT_COMET), exact = true),
            Golden("Lusitano!", expected = setOf(PATIENT_BELLA), exact = true),
            // --- Near-duplicate name disambiguation ---
            Golden("Thunderstorm", expected = setOf(PATIENT_THUNDERSTORM), exact = true),
            Golden("Thunder vs Thunderstorm", expected = setOf(PATIENT_THUNDER, PATIENT_THUNDERSTORM), exact = true),
            // Substring is NOT enough: "bella*" is token-start anchored and
            // must not reach "Isabella".
            Golden("Bella", expected = setOf(PATIENT_BELLA), exact = true),
            Golden("Isabella", expected = setOf(PATIENT_ISABELLA), exact = true),
            Golden("Belinha", expected = setOf(PATIENT_BELINHA), exact = true),
            Golden("Trovoada", expected = setOf(PATIENT_TROVOADA), exact = true),
            Golden("Comet", expected = setOf(PATIENT_COMET), exact = true),
            // --- Weight values (series completeness) ---
            Golden("525", expected = setOf(WEIGHT_525), exact = true),
            Golden("538", expected = setOf(WEIGHT_538), exact = true),
            Golden("495", expected = setOf(WEIGHT_495), exact = true),
            Golden("410", expected = setOf(WEIGHT_410), exact = true),
            Golden("380", expected = setOf(WEIGHT_380), exact = true),
            Golden("290", expected = setOf(WEIGHT_290), exact = true),
            // gestationDays is never indexed (only status+notes+vocab).
            Golden("341", expected = emptySet(), exact = true),
            // Shared numbers span types through numeric prefixes.
            Golden("250", expected = setOf(LAB_FECAL), exact = true),
            Golden("400", expected = setOf(LAB_CBC, DEWORM_MOXIDECTIN), exact = true),
            Golden("200", expected = setOf(DEWORM_IVERMECTIN, LAB_FECAL), exact = true),
            Golden("10", expected = setOf(SUBSTANCE_DETOMIDINE, LAB_CBC, DEWORM_FENBENDAZOLE, DEWORM_PYRANTEL, "MEDICATION#$medication2Id"), exact = true),
            // --- Broad single tokens (recall-breadth documentation) ---
            Golden("rest", expected = setOf(CONSULT_COUGH, LAMENESS_NAVICULAR, SURGERY_ARTHROSCOPY), exact = true),
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
     * Locks the CURRENT relevance ordering (`ORDER BY bm25(SearchFts),
     * i.date DESC`) for a tie-free query. Ranking changes must update this
     * expectation deliberately.
     */
    @Test
    fun relevanceOrderWilson() {
        val orderedKeys = retrieve("Wilson").map { it.key() }
        assertEquals(
            listOf(
                // BM25 relevance, best first: the single term "wilson" appears
                // once in every hit, so shorter documents score higher; no
                // date ties occur so the DESC tiebreak stays latent here.
                IMAGING_KNEE, // Bella - shortest indexed text
                LAB_COGGINS, // Bella
                DEWORM_FENBENDAZOLE, // Bella
                DENTISTRY_FLOATING, // Thunder
                US_BELLA_FOLLICLE, // Bella
                VACC_WEST_NILE, // Bella - longest of the short rows (v7 vocabulary)
                CONSULT_COUGH, // Comet - full SOAP text, lowest rank
            ),
            orderedKeys,
            "relevance order changed - update this baseline deliberately with the ranking change",
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

    /** May window over the follicle token scopes to the May-dated rows. */
    @Test
    fun dateWindowFollicleMay() {
        val may =
            searchUseCase("follicle", from = LocalDate(2026, 5, 1), to = LocalDate(2026, 5, 31), recordTypes = null)
                .map { it.key() }
                .toSet()
        assertEquals(setOf(US_BELLA_FOLLICLE, REPRO_HEAT), may)
    }

    /** Gestation rows are dated by BREEDING date, not lastCheck/status. */
    @Test
    fun dateWindowGestationUsesBreedingDate() {
        val all = searchUseCase("filly", from = null, to = null, recordTypes = null).map { it.key() }
        assertEquals(listOf(GESTATION_COMPLETED), all)

        // Bred 2025-04-01, so a 2026+ window excludes it even though the
        // foaling happened in 2026.
        val from2026 =
            searchUseCase("filly", from = LocalDate(2026, 1, 1), to = null, recordTypes = null)
        assertTrue(from2026.isEmpty(), "gestation row must be dated by breedingDate (2025)")
    }

    /** Vaccination window uses dateAdministered; vocabulary hits filter too. */
    @Test
    fun dateWindowVaccinationsJanFeb() {
        val janFeb =
            searchUseCase("booster", from = LocalDate(2026, 1, 1), to = LocalDate(2026, 2, 28), recordTypes = null)
                .map { it.key() }
                .toSet()
        assertEquals(setOf(VACC_INFLUENZA, VACC_TETANUS, VACC_EHV), janFeb)
    }

    /** July window isolates the ICSI row; medication rows (date=null) drop out. */
    @Test
    fun dateWindowJulyOocytes() {
        val july =
            searchUseCase("oocytes", from = LocalDate(2026, 7, 1), to = null, recordTypes = null)
                .map { it.key() }
        assertEquals(listOf(ICSI_COMET), july)
    }

    /** March window isolates the Rabies vaccination by its indexed date. */
    @Test
    fun dateWindowMarchRabies() {
        val march =
            searchUseCase("rabies", from = LocalDate(2026, 3, 1), to = LocalDate(2026, 3, 31), recordTypes = null)
                .map { it.key() }
        assertEquals(listOf(VACC_RABIES), march)
    }

    /** The seeded fixture covers every indexed row kind exactly once. */
    @Test
    fun fixtureCoversAllIndexedRows() {
        // 7 patients + 4 owners + 53 clinical/repro/medication records
        // (29 from the first wave, 24 from the disambiguation wave).
        val expectedRows = 64
        assertEquals(expectedRows.toLong(), database.searchFtsQueries.countIndexRows().executeAsOne())
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun SearchResult.key(): String = "$recordType#$recordId"

    /**
     * Mirrors the production retrieval path in [GenerateRagResponseUseCase]:
     * filler-stripped AND query first, one broad OR retry (with synonym
     * expansion) when the AND query is empty OR WEAK (fewer than
     * [WEAK_RESULT_THRESHOLD] records), with retry hits deduplicated against
     * the AND leg by record identity.
     *
     * The OR leg goes through [SearchRepositoryImpl] directly with an
     * FTS-safe expression from [AssistantPrompts.toFtsOrQuery]:
     * [SearchUseCase] star-joins every whitespace token, so an OR-joined
     * fallback becomes `a* AND OR* AND b*` and FTS5 rejects the reserved
     * operator carrying a suffix star (retrieval bug fixed in the llm lane).
     */
    private fun retrieve(question: String): List<SearchResult> {
        val andResults =
            searchUseCase(AssistantPrompts.enrichQuery(question), from = null, to = null, recordTypes = null)
        if (andResults.size >= WEAK_RESULT_THRESHOLD) return andResults
        val retryResults = searchRepo.search(AssistantPrompts.toFtsOrQuery(question), null, null, null)
        return andResults +
            retryResults.filter { retry ->
                andResults.none { it.recordType == retry.recordType && it.recordId == retry.recordId }
            }
    }
}
