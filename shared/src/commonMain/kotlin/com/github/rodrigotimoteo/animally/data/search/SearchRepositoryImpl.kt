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
            searchQueries.insertFts(searchableText).value
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
            val searchableText = listOfNotNull(it.status, it.notes).joinToString(" ")
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

    /** Appends prefix stars to plain tokens, preserving explicit FTS syntax.
     * Non-alphanumeric characters (hyphens, punctuation) split tokens — FTS5
     * tokenizes content the same way, so "UITest-DF4D25" becomes
     * "uitest* df4d25*", matching how the text was indexed. */
    private fun toPrefixMatchQuery(query: String): String =
        query
            .split(Regex("\\s+"))
            .flatMap { token ->
                val isSyntax = token.endsWith("*") || token.uppercase() in setOf("AND", "OR", "NOT")
                if (isSyntax) {
                    listOf(token)
                } else {
                    token
                        .split(Regex("[^\\p{L}\\p{N}]+"))
                        .filter { it.isNotBlank() }
                        .map { "$it*" }
                }
            }.filter { it.isNotBlank() }
            .joinToString(" ")

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
        reindexOwners()
        reindexPatients()
        reindexRecords()
        rebuild()
        database.searchIndexStateQueries.upsertState(VERSION_KEY, indexVersion)
    }

    private companion object {
        const val VERSION_KEY = "search_index_version"
    }
}
