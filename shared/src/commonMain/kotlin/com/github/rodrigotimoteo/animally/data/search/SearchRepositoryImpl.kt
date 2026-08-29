package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.customreminder.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.embryotransfer.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.icsi.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.data.owner.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.patient.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.search.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText
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
@Suppress("TooManyFunctions") // One adapter owns the cross-record FTS index.
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

    override fun searchSnippets(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
        // Same sanitization contract as [search]: accepts raw or already
        // FTS-shaped input (the sanitizer is idempotent on shaped input).
        val matchQuery = toPrefixMatchQuery(query)
        if (matchQuery.isBlank()) return emptyList()
        val recordHits =
            searchQueries
                .searchSnippets(matchQuery, from, to)
                .executeAsList()
                .filter { recordTypes == null || it.recordType in recordTypes }
                .map { it.toDomain() }
        // Owner text (name/email/phone/address) is short by construction;
        // the full-text owner query needs no snippet window.
        val ownerHits =
            if (recordTypes == null || ISearchRepository.TYPE_OWNER in recordTypes) {
                searchQueries.searchOwners(matchQuery).executeAsList().map { it.toDomain() }
            } else {
                emptyList()
            }
        return recordHits + ownerHits
    }

    override fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> =
        searchQueries
            .searchByDateRange(from, to)
            .executeAsList()
            .map { it.toDomain() }

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
                val searchableText = SearchableText.owner(owner.toDomain())
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
                val searchableText = SearchableText.patient(patient.toDomain())
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
        reindexAnamneseRows()
        reindexVaccinationRows()
        reindexConsultationRows()
        reindexMedicationRows()
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
        reindexCustomReminderRows()
    }

    private val reindexAnamneseRows: () -> Unit = {
        database.anamneseQueries.selectAllRows().executeAsList().forEach {
            val anamnese = it.toDomain()
            indexRecord(
                recordType = RecordType.Anamnese.wireName,
                patientId = anamnese.patientId,
                recordId = anamnese.id,
                date = null,
                searchableText = SearchableText.anamnese(anamnese),
            )
        }
    }

    private val reindexConsultationRows: () -> Unit = {
        database.consultationQueries.selectAll().executeAsList().forEach {
            val searchableText = SearchableText.consultation(it.toDomain())
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
            val searchableText = SearchableText.vaccination(it.toDomain())
            indexRecord(
                recordType = RecordType.Vaccination.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.dateAdministered,
                searchableText = searchableText,
            )
        }
    }

    private val reindexMedicationRows: () -> Unit = {
        database.medicationQueries.selectAll().executeAsList().forEach {
            val medication = it.toDomain()
            indexRecord(
                recordType = RecordType.Medication.wireName,
                patientId = medication.patientId,
                recordId = medication.id,
                date = null,
                searchableText = SearchableText.medication(medication),
            )
        }
    }

    private val reindexDewormingRows: () -> Unit = {
        database.dewormingQueries.selectAll().executeAsList().forEach {
            val searchableText = SearchableText.deworming(it.toDomain())
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
            val searchableText = SearchableText.dentistry(it.toDomain())
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
            val searchableText = SearchableText.farrierVisit(it.toDomain())
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
            val searchableText = SearchableText.lameness(it.toDomain())
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
            val searchableText = SearchableText.surgery(it.toDomain())
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
            val searchableText = SearchableText.controlledSubstance(it.toDomain())
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
            val searchableText = SearchableText.weight(it.toDomain())
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
            val searchableText = SearchableText.reproductionEvent(it.toDomain())
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
            val searchableText = SearchableText.ultrasound(it.toDomain())
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
            val searchableText = SearchableText.gestation(it.toDomain())
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
            val searchableText = SearchableText.reproMedication(it.toDomain())
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
            val searchableText = SearchableText.labResult(it.toDomain())
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
            val searchableText = SearchableText.imaging(it.toDomain())
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
            val searchableText = SearchableText.embryoTransfer(it.toDomain())
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
            val searchableText = SearchableText.icsi(it.toDomain())
            indexRecord(
                recordType = RecordType.Icsi.wireName,
                patientId = it.patientId,
                recordId = it.id,
                date = it.date,
                searchableText = searchableText,
            )
        }
    }

    private val reindexCustomReminderRows: () -> Unit = {
        database.customReminderQueries.selectAllActive().executeAsList().forEach {
            val reminder = it.toDomain()
            indexRecord(
                recordType = RecordType.CustomReminder.wireName,
                patientId = reminder.patientId,
                recordId = reminder.id,
                date = reminder.dueDate,
                searchableText = SearchableText.customReminder(reminder),
            )
        }
    }

    /** Builds a safe FTS5 MATCH query from raw user input.
     * Plain tokens are split on non-alphanumeric characters (hyphens,
     * punctuation — FTS5 tokenizes content the same way) and each surviving
     * word gets a trailing prefix star, so "thun" finds "Thunder".
     * SHORT-PREFIX GUARD: alphabetic words shorter than
     * [MIN_PREFIX_STAR_CHARS] match EXACTLY (quoted, starless) instead of
     * star-joining — a bare "da*" explodes onto every da- token in the
     * corpus (Daniela, daily, days, "day 30"), polluting retrieval with
     * hundreds of weak hits. Numeric tokens keep the star at any length: the
     * weight-series UX types "5" to surface 512/525/538, and digit prefixes
     * stay useful where 2-letter alphabetic prefixes are pure noise.
     * Quoted segments become quoted FTS phrases with a trailing star
     * (FTS5 applies the prefix to the phrase's final token), preserving the
     * exact word sequence — subject to the SAME short-final-token guard as
     * bare tokens, so "barn b" matches the exact phrase instead of emitting
     * a b* explosion. Internal asterisks are stripped everywhere — only
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
                            .map(::starOrExact)
                    }
                }
            }.joinToString(" ")

    /**
     * True when [word] is safe to star-join as a prefix: long enough, or
     * numeric (digit prefixes stay useful where 2-letter alphabetic ones are
     * pure noise). Shared by bare tokens and quoted phrases' final token.
     */
    private fun keepsPrefixStar(word: String): Boolean = word.length >= MIN_PREFIX_STAR_CHARS || word.all(Char::isDigit)

    /**
     * Stars [word] for prefix matching, or renders it as a quoted exact term
     * when it is too short to star-join safely (alphabetic words under
     * [MIN_PREFIX_STAR_CHARS]); numeric words always keep the star.
     */
    private fun starOrExact(word: String): String = if (keepsPrefixStar(word)) "$word*" else "\"$word\""

    /**
     * Renders a quoted segment as a quoted FTS phrase with a trailing prefix
     * star (FTS5 applies the star to the phrase's FINAL token), or null when
     * no words survive sanitization. The final token gets the same
     * short-prefix guard as bare tokens ([starOrExact]): a quoted "barn b"
     * would otherwise emit `"barn b"*` and b* explodes onto every b- token,
     * so short final tokens match the exact phrase instead.
     */
    private fun toQuotedPhrase(content: String): String? {
        val words =
            content
                .replace("*", "")
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val body = "\"${words.joinToString(" ")}\""
        return if (keepsPrefixStar(words.last())) "$body*" else body
    }

    private companion object {
        const val VERSION_KEY = "search_index_version"

        // Minimum alphabetic word length for prefix star-joining; shorter
        // words match exactly instead (see [toPrefixMatchQuery]).
        const val MIN_PREFIX_STAR_CHARS = 3

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
        // Clear both tables first so rows for records that were deleted since
        // the previous healing pass cannot survive as stale search results.
        suppressFtsWrites = true
        try {
            database.transaction {
                searchQueries.deleteAllFts().value
                searchQueries.deleteAllIndex().value
            }
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
