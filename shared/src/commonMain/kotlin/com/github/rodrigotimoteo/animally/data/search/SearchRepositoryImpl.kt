package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.data.search.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Repository implementation managing the FTS5 global search index.
 *
 * Writes are applied inside a transaction so the metadata table
 * ([SearchFtsIndex]) and the FTS index ([SearchFts]) stay consistent.
 * The FTS rowid is kept aligned with the [SearchFtsIndex] id.
 */
@Single(binds = [ISearchRepository::class])
class SearchRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
    @Provided private val ownerQueries: OwnerQueries,
) : ISearchRepository {
    private val searchQueries: SearchFtsQueries = database.searchFtsQueries

    /**
     * Suppresses save-time FTS writes while the bulk heal runs: only the
     * metadata table is written; [rebuild] performs the single FTS build
     * afterwards. Save-time single-record indexing keeps writing both tables.
     *
     * Confined to the main thread by convention: reindexIfNeeded is invoked
     * once at app start and every indexRecord caller is UI-driven on the same
     * thread. A future background writer would need real synchronization here.
     */
    private var suppressFtsWrites = false

    override fun indexRecord(
        recordType: String,
        patientId: Long,
        recordId: Long,
        date: LocalDate?,
        searchableText: String,
    ) {
        database.transaction {
            removeIndexRow(recordType, recordId)
            searchQueries.insertIndex(recordType, patientId, recordId, date, searchableText).value
            if (!suppressFtsWrites) {
                searchQueries.insertFts(searchableText).value
            }
        }
    }

    override fun deleteRecord(
        recordType: String,
        recordId: Long,
    ) {
        database.transaction {
            removeIndexRow(recordType, recordId)
        }
    }

    override fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
        // FTS5 MATCH is token-exact; trailing each plain token with '*' turns
        // the query into a prefix match so partial input ("thun") finds
        // "Thunder". Tokens that are already FTS-syntax (starred terms, boolean
        // operators) pass through untouched.
        val matchQuery = toPrefixMatchQuery(query)
        if (matchQuery.isBlank()) return emptyList()
        val recordHits =
            searchQueries
                .search(matchQuery, from, to)
                .executeAsList()
                .filter { recordTypes == null || it.recordType in recordTypes }
                .map { it.toDomain() }
        val ownerHits =
            if (recordTypes == null || ISearchRepository.TYPE_OWNER in recordTypes) {
                searchQueries.searchOwners(matchQuery).executeAsList().map { it.toDomain() }
            } else {
                emptyList()
            }
        return recordHits + ownerHits
    }

    override fun rebuild() {
        database.transaction {
            searchQueries.deleteAllFts().value
            searchQueries.reseed().value
        }
    }

    override fun reindexOwners() {
        ownerQueries
            .selectAll()
            .executeAsList()
            .forEach { owner ->
                val searchableText =
                    listOfNotNull(owner.name, owner.email, owner.phone, owner.address)
                        .joinToString(" ")
                indexRecord(
                    recordType = ISearchRepository.TYPE_OWNER,
                    patientId = 0L,
                    recordId = owner.id,
                    date = null,
                    searchableText = searchableText,
                )
            }
    }

    override fun reindexPatients() {
        database.patientQueries
            .selectAll()
            .executeAsList()
            .forEach { patient ->
                val searchableText =
                    listOfNotNull(
                        patient.name,
                        patient.species,
                        patient.breed,
                        patient.microchipId,
                        patient.ueln,
                        patient.registrationNumber,
                        patient.stableLocation,
                    ).joinToString(" ")
                indexRecord(
                    recordType = ISearchRepository.TYPE_PATIENT,
                    patientId = patient.id,
                    recordId = patient.id,
                    date = null,
                    searchableText = searchableText,
                )
            }
    }

    override fun reindexRecords() {
        reindexVaccinationRows()
        reindexConsultationRows()
        reindexDewormingRows()
        reindexDentistryRows()
        reindexFarrierVisitRows()
        reindexLamenessRows()
        reindexSurgeryRows()
        reindexControlledSubstanceRows()
        reindexWeightRows()
        reindexReproductionEventRows()
        reindexUltrasoundRows()
        reindexGestationRows()
        reindexReproMedicationRows()
        reindexLabResultRows()
        reindexImagingRows()
        reindexEmbryoTransferRows()
        reindexIcsiRows()
    }

    private val reindexConsultationRows: () -> Unit = {
        database.consultationQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.subjective,
                    it.objective,
                    it.assessment,
                    it.plan,
                    it.vetName,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Consultation.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexVaccinationRows: () -> Unit = {
        database.vaccinationQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.vaccineName,
                    it.batchNumber,
                    it.vetName,
                    it.site,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Vaccination.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.dateAdministered,
                searchableText = searchableText,
            )
        }
    }

    private val reindexDewormingRows: () -> Unit = {
        database.dewormingQueries.selectAll().executeAsList().forEach {
            val searchableText = listOfNotNull(it.product, it.dose, it.vetName, it.notes).joinToString(" ")
            indexRecord(
                recordType = RecordType.Deworming.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.dateAdministered,
                searchableText = searchableText,
            )
        }
    }

    private val reindexDentistryRows: () -> Unit = {
        database.dentistryQueries.selectAll().executeAsList().forEach {
            val searchableText = listOfNotNull(it.findings, it.treatment, it.vetName, it.notes).joinToString(" ")
            indexRecord(
                recordType = RecordType.Dentistry.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexFarrierVisitRows: () -> Unit = {
        database.farrierVisitQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.trimOrShoe,
                    it.shoeType,
                    it.findings,
                    it.farrier,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.FarrierVisit.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexLamenessRows: () -> Unit = {
        database.lamenessQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.gradeAAEP.toString(),
                    it.limbLocation,
                    it.flexionTest,
                    it.diagnosis,
                    it.treatment,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Lameness.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexSurgeryRows: () -> Unit = {
        database.surgeryQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.type,
                    it.description,
                    it.outcome,
                    it.surgeon,
                    it.anesthesia,
                    it.analgesia,
                    it.complications,
                    it.recoveryNotes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Surgery.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexControlledSubstanceRows: () -> Unit = {
        database.substanceQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.drugName,
                    it.dose,
                    it.unit,
                    it.route,
                    it.administeredBy,
                    it.witness,
                    it.reason,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.ControlledSubstance.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexWeightRows: () -> Unit = {
        database.weightQueries.selectAll().executeAsList().forEach {
            val searchableText = listOfNotNull(it.weightKg.toString(), it.notes).joinToString(" ")
            indexRecord(
                recordType = RecordType.Weight.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexReproductionEventRows: () -> Unit = {
        database.reproductionQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.eventType,
                    it.details,
                    it.initialExamFindings,
                    it.stallionName,
                    it.breedingType,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.ReproductionEvent.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexUltrasoundRows: () -> Unit = {
        database.ultrasoundQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.ovaryStatus,
                    it.uterineStatus,
                    it.leftOvaryStatus,
                    it.rightOvaryStatus,
                    it.uterineEdema,
                    it.uterineLiquidDescription,
                    it.uterusDescription,
                    it.findings,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Ultrasound.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexGestationRows: () -> Unit = {
        database.gestationQueries.selectAll().executeAsList().forEach {
            // Natural questions ("is she pregnant?") must hit unresolved
            // gestations, whose raw fields (status "Active", notes) may not
            // contain those words. Resolved pregnancies (Completed/Failed,
            // mirroring GetUpcomingRemindersUseCase) must NOT claim an active
            // pregnancy, so they keep only their own indexed text.
            val isResolved =
                it.status.equals("Completed", ignoreCase = true) ||
                    it.status.equals("Failed", ignoreCase = true)
            val pregnancyVocabulary =
                if (isResolved) null else "pregnant in foal active gestation expected foaling"
            val searchableText = listOfNotNull(it.status, it.notes, pregnancyVocabulary).joinToString(" ")
            indexRecord(
                recordType = RecordType.Gestation.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.breedingDate,
                searchableText = searchableText,
            )
        }
    }

    private val reindexReproMedicationRows: () -> Unit = {
        database.reproMedicationQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.medication,
                    it.dosage,
                    it.purpose,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.ReproMedication.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.dateAdministered,
                searchableText = searchableText,
            )
        }
    }

    private val reindexLabResultRows: () -> Unit = {
        database.labResultQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.testType,
                    it.results,
                    it.normalRange,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.LabResult.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexImagingRows: () -> Unit = {
        database.imagingQueries.selectAll().executeAsList().forEach {
            val searchableText = listOfNotNull(it.type, it.findings, it.vetName, it.notes).joinToString(" ")
            indexRecord(
                recordType = RecordType.Imaging.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexEmbryoTransferRows: () -> Unit = {
        database.embryoTransferQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.embryoCount?.toString(),
                    it.recipientMares,
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.EmbryoTransfer.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexIcsiRows: () -> Unit = {
        database.icsiQueries.selectAll().executeAsList().forEach {
            val searchableText =
                listOfNotNull(
                    it.folliclesRecovered?.toString(),
                    it.vetName,
                    it.notes,
                ).joinToString(" ")
            indexRecord(
                recordType = RecordType.Icsi.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    /** Builds a safe FTS5 MATCH query from raw user input.
     * Plain tokens are split on non-alphanumeric characters (hyphens,
     * punctuation — FTS5 tokenizes content the same way) and each surviving
     * word gets a trailing prefix star, so "thun" finds "Thunder".
     * Quoted segments become quoted FTS phrases with a trailing star
     * (FTS5 applies the prefix to the phrase's final token), preserving the
     * exact word sequence. Internal asterisks are stripped everywhere — only
     * the trailing wildcard this function appends is ever emitted. Bare
     * uppercase AND/OR/NOT pass through as boolean operators. Returns an
     * empty string when nothing survives sanitization; callers treat that as
     * "no results". */
    private fun toPrefixMatchQuery(query: String): String =
        queryPartRegex
            .findAll(query)
            .flatMap { match ->
                val quoted = match.groupValues[1]
                if (match.groupValues[2].isEmpty()) {
                    listOfNotNull(toQuotedPhrase(quoted))
                } else {
                    val token = match.groupValues[2]
                    if (token.uppercase() in BOOLEAN_OPERATORS) {
                        listOf(token)
                    } else {
                        token
                            .replace("*", "")
                            .split(Regex("[^\\p{L}\\p{N}]+"))
                            .filter { it.isNotBlank() }
                            .map { "$it*" }
                    }
                }
            }.joinToString(" ")

    /** Renders a quoted segment as a quoted FTS phrase with a trailing
     * prefix star, or null when no words survive sanitization. */
    private fun toQuotedPhrase(content: String): String? {
        val words =
            content
                .replace("*", "")
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        return "\"${words.joinToString(" ")}\"*"
    }

    private companion object {
        const val VERSION_KEY = "search_index_version"

        // Group 1: a fully quoted segment (may contain spaces). Group 2: any
        // other whitespace-delimited run (including unmatched lone quotes,
        // which then sanitize as plain tokens).
        val queryPartRegex = Regex("\"([^\"]*)\"|(\\S+)")

        val BOOLEAN_OPERATORS = setOf("AND", "OR", "NOT")
    }

    private fun removeIndexRow(
        recordType: String,
        recordId: Long,
    ) {
        val existing = searchQueries.selectIndexRow(recordType, recordId).executeAsOneOrNull()
        searchQueries.deleteIndex(recordType, recordId).value
        if (existing != null) {
            searchQueries.deleteFts(existing.id).value
        }
    }

    override fun reindexIfNeeded(indexVersion: String) {
        val storedVersion =
            database.searchIndexStateQueries
                .selectState(VERSION_KEY)
                .executeAsOneOrNull()
        val hasIndexRows = searchQueries.countIndexRows().executeAsOne() > 0L
        if (storedVersion == indexVersion && hasIndexRows) {
            return
        }
        // Single-pass bulk reindex: the reindex*Rows calls write only the
        // metadata table; rebuild() then does the one FTS build from it.
        suppressFtsWrites = true
        try {
            reindexOwners()
            reindexPatients()
            reindexRecords()
        } finally {
            suppressFtsWrites = false
        }
        rebuild()
        database.searchIndexStateQueries.upsertState(VERSION_KEY, indexVersion)
    }
}
