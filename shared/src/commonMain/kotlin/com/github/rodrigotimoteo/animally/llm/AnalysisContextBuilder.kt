package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GestationProgress
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.llm.support.DateFormatting
import com.github.rodrigotimoteo.animally.llm.support.SharedStopWords
import com.github.rodrigotimoteo.animally.llm.support.TokenEstimator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Database-backed gestation facts projected for a single assistant turn. */
internal data class GestationFact(
    val patient: Patient,
    val gestation: Gestation,
    val progress: GestationProgress,
    /** Elapsed days from the recorded breeding date to the turn's reference date. */
    val elapsedDays: Int,
    val isActive: Boolean,
)

/** Database-backed breeding-card fact projected for a single assistant turn. */
internal data class BreedingFact(
    val patient: Patient,
    val event: ReproductionEvent,
    /** Elapsed days from the recorded breeding-card date to the turn date. */
    val elapsedDays: Int,
)

/** Reproduction-card facts used to answer breeding-outcome questions exactly. */
internal data class BreedingOutcomeFact(
    val patient: Patient,
    val event: ReproductionEvent,
)

/**
 * Deterministic analysis context for the assistant: Kotlin COMPUTES, the
 * model NARRATES. A 4B-parameter on-device model must never do arithmetic,
 * so count/list/trend questions get compact summary blocks computed here
 * from the repositories and prepended to the RAG prompt as authoritative
 * facts the model can cite as [Summary] but never contradict.
 *
 * Each block stays around ~100 tokens; the whole summary is capped at
 * [MAX_SUMMARY_TOKENS] so summaries share the RagConfig prompt budget
 * predictably. Queries without analysis intent return null and skip the
 * repository scan entirely.
 */
@Suppress("TooManyFunctions") // One deterministic block per supported record analysis.
class AnalysisContextBuilder(
    private val patientRepository: IPatientRepository,
    private val weightRepository: IWeightRepository,
    private val vaccinationRepository: IVaccinationRepository,
    private val dewormingRepository: IDewormingRepository,
    private val farrierVisitRepository: IFarrierVisitRepository,
    private val gestationRepository: IGestationRepository,
    private val reproductionRepository: IReproductionRepository? = null,
    private val calculateGestationUseCase: CalculateGestationUseCase = CalculateGestationUseCase(),
) {
    private data class AnalysisWeightRow(
        val patient: Patient,
        val weight: Weight,
    )

    private data class CarePatientSummary(
        val patient: Patient,
        val vaccinations: List<Vaccination>,
        val dewormings: List<Deworming>,
        val farrierVisits: List<FarrierVisit>,
    ) {
        val hasRecords: Boolean
            get() = vaccinations.isNotEmpty() || dewormings.isNotEmpty() || farrierVisits.isNotEmpty()
    }

    private data class GestationSummaryRow(
        val patient: Patient,
        val gestation: Gestation,
    )

    /**
     * Builds the deterministic summary for [query], or null when the query
     * carries no analysis intent. [today] anchors overdue filtering and
     * gestation day counts; production defaults to the device clock while
     * tests pass a fixed date.
     */
    fun build(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): String? {
        if (!AnalysisIntents.isAnalysisQuery(query)) return null
        val patients = patientRepository.getPatientList()
        val matchedPatients = patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val dateRange = RagDateRangeIntent.resolve(query, today)
        val hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query)
        val hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query)
        val careTargets =
            resolveCareTargets(
                patients,
                matchedPatients,
                scoped,
                hasIndividualReference,
                hasLikelyName,
            )
        val blocks =
            buildList {
                if (AnalysisIntents.wantsCensus(query)) add(censusBlock(patients))
                if (AnalysisIntents.wantsWeight(query)) weightTrendBlock(careTargets, dateRange)?.let(::add)
                if (AnalysisIntents.wantsCareCounts(query)) careBlock(careTargets, dateRange)?.let(::add)
                if (AnalysisIntents.wantsGestation(query)) gestationBlock(careTargets, today)?.let(::add)
                if (AnalysisIntents.wantsOverdue(query)) overdueBlock(careTargets, today)?.let(::add)
            }
        return assemble(blocks)
    }

    /**
     * Returns the gestation rows relevant to [query], with live progress for
     * active pregnancies. A non-null result means the query is about
     * gestation; an empty list is meaningful and prevents a model from
     * turning missing pregnancy data into a confident answer.
     */
    internal fun gestationFacts(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<GestationFact>? {
        if (!AnalysisIntents.wantsCurrentGestation(query)) return null
        val patients = patientRepository.getPatientList()
        val matchedPatients = patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val careTargets =
            resolveCareTargets(
                patients = patients,
                matchedPatients = matchedPatients,
                scoped = scoped,
                hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query),
                hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query),
            )
        return careTargets
            .flatMap { patient ->
                gestationRepository.getByPatient(patient.id).map { gestation ->
                    val active = gestation.isActiveGestation()
                    val currentProgress = calculateGestationUseCase(gestation.breedingDate, today)
                    GestationFact(
                        patient = patient,
                        gestation = gestation,
                        progress =
                            if (active) {
                                currentProgress
                            } else {
                                GestationProgress(gestation.expectedDueDate, gestation.gestationDays)
                            },
                        elapsedDays = currentProgress.gestationDays,
                        isActive = active,
                    )
                }
            }.sortedWith(compareBy({ !it.isActive }, { it.gestation.breedingDate }, { it.patient.name.lowercase() }))
    }

    /**
     * Returns breeding-card facts for timing questions. Reproduction events
     * are checked before gestation rows because the card is the source of
     * truth for the actual breeding date; the two records can differ by a
     * day when a pregnancy was entered or corrected later.
     */
    internal fun breedingFacts(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<BreedingFact>? {
        if (!AnalysisIntents.wantsBreedingTiming(query)) return null
        val repository = reproductionRepository ?: return emptyList()
        val patients = patientRepository.getPatientList()
        val matchedPatients = patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val careTargets =
            resolveCareTargets(
                patients = patients,
                matchedPatients = matchedPatients,
                scoped = scoped,
                hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query),
                hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query),
            )
        return careTargets
            .flatMap { patient ->
                repository
                    .getByPatient(patient.id)
                    .filter { event -> event.isActive && event.isBreedingEvent() }
                    .map { event ->
                        BreedingFact(
                            patient = patient,
                            event = event,
                            elapsedDays = calculateGestationUseCase(event.date, today).gestationDays,
                        )
                    }
            }.sortedWith(compareByDescending<BreedingFact> { it.event.date }.thenBy { it.patient.name.lowercase() })
    }

    /** Returns the recorded reproductive timeline for an outcome question. */
    internal fun reproductionOutcomeFacts(query: String): List<BreedingOutcomeFact>? {
        if (!AnalysisIntents.wantsBreedingOutcome(query)) return null
        val repository = reproductionRepository ?: return null
        val patients = patientRepository.getPatientList()
        val matchedPatients = patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val careTargets =
            resolveCareTargets(
                patients = patients,
                matchedPatients = matchedPatients,
                scoped = scoped,
                hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query),
                hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query),
            )
        return careTargets
            .flatMap { patient ->
                repository.getByPatient(patient.id).map { event -> BreedingOutcomeFact(patient, event) }
            }.sortedWith(compareBy<BreedingOutcomeFact> { it.event.date }.thenBy { it.patient.name.lowercase() })
    }

    /** Returns exact reproduction-card fields such as the recorded stallion. */
    internal fun reproductionAttributeFacts(query: String): List<ReproductionAttributeFact>? {
        if (ReproductionAttributeIntent.requestedAttribute(query) == null) return null
        val repository = reproductionRepository ?: return emptyList()
        val patients = patientRepository.getPatientList()
        val matchedPatients = patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val careTargets =
            resolveCareTargets(
                patients = patients,
                matchedPatients = matchedPatients,
                scoped = scoped,
                hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query),
                hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query),
            )
        return careTargets
            .flatMap { patient ->
                repository
                    .getByPatient(patient.id)
                    .filter { event -> event.isActive && event.isBreedingEvent() }
                    .map { event -> ReproductionAttributeFact(patient, event) }
            }.sortedWith(
                compareByDescending<ReproductionAttributeFact> { it.event.date }
                    .thenBy { it.patient.name.lowercase() },
            )
    }

    /**
     * Patients whose names contain an exact query token (case-insensitive,
     * possessives stripped). The caller treats multiple matches as ambiguous
     * so summaries and retrieval agree on which patient the question is about.
     * Prefix matching is intentionally avoided: "Ann" must not select
     * "Annabelle" and expose the wrong patient's measurements.
     */
    private fun patientNameMatches(
        patients: List<Patient>,
        query: String,
    ): List<Patient> {
        val tokens =
            query
                .split(Regex("\\s+"))
                .map {
                    it
                        .trim('?', ',', '.', '!', ':', ';', '\'')
                        .removeSuffix("'s")
                        .removeSuffix("’s")
                }.filter {
                    it.length >= MIN_NAME_PREFIX_CHARS &&
                        it.lowercase() !in SharedStopWords.PATIENT_SCOPE_STOP_WORDS
                }.map(String::lowercase)
                .toSet()
        if (tokens.isEmpty()) return emptyList()
        return patients
            .filter { patient ->
                val nameTokens =
                    patient.name
                        .split(Regex("\\s+"))
                        .map {
                            it
                                .trim('?', ',', '.', '!', ':', ';', '\'')
                                .removeSuffix("'s")
                                .removeSuffix("’s")
                                .lowercase()
                        }.toSet()
                tokens.any { token -> token in nameTokens }
            }
    }

    private fun resolveCareTargets(
        patients: List<Patient>,
        matchedPatients: List<Patient>,
        scoped: Patient?,
        hasIndividualReference: Boolean,
        hasLikelyName: Boolean,
    ): List<Patient> =
        when {
            scoped != null -> listOf(scoped)
            matchedPatients.isEmpty() && hasIndividualReference && !hasLikelyName && patients.size == 1 ->
                listOf(patients.single())
            matchedPatients.isNotEmpty() || hasIndividualReference || hasLikelyName -> emptyList()
            else -> patients
        }

    /** Active patient count plus names - answers "how many patients" exactly. */
    private fun censusBlock(patients: List<Patient>): String {
        val names = patients.take(MAX_NAMES_IN_CENSUS).joinToString(", ") { it.name }
        val overflow = if (patients.size > MAX_NAMES_IN_CENSUS) " …" else ""
        return "PATIENT CENSUS: ${patients.size} active ${pluralize("patient", patients.size)}: $names$overflow."
    }

    /**
     * Weight trend for one patient: series min/max/latest with dates and a
     * direction derived from the two most recent measurements. A single
     * entry has no trend, so it is reported as one measurement instead of
     * inventing min == max == latest noise.
     */
    private fun weightTrendBlock(
        targets: List<Patient>,
        dateRange: RagDateRange?,
    ): String? {
        val rows =
            targets
                .flatMap { patient ->
                    weightRepository
                        .getByPatient(patient.id)
                        .filter { weight -> dateRange?.contains(weight.date) != false }
                        .map { weight -> AnalysisWeightRow(patient, weight) }
                }.sortedWith(compareBy({ it.weight.date }, { it.patient.name.lowercase() }, { it.weight.id }))
        if (rows.isEmpty()) return null
        val rowsByPatient = rows.groupBy { it.patient.id }
        if (rowsByPatient.size == 1) {
            return weightTrendLine(rows.first().patient, rows.map(AnalysisWeightRow::weight))
        }

        val values = rows.map { it.weight.weightKg }
        val minimum = rows.minBy { it.weight.weightKg }
        val maximum = rows.maxBy { it.weight.weightKg }
        val details =
            rowsByPatient
                .values
                .sortedBy {
                    it
                        .first()
                        .patient.name
                        .lowercase()
                }.take(MAX_PATIENTS_SCANNED)
                .map { patientRows ->
                    weightTrendLine(patientRows.first().patient, patientRows.map(AnalysisWeightRow::weight))
                }
        val omitted = rowsByPatient.size - details.size
        return buildString {
            appendLine(
                "WEIGHT SUMMARY: ${rows.size} measurements across ${rowsByPatient.size} patients; " +
                    "average ${values.average()} kg, median ${median(values)} kg, " +
                    "minimum ${minimum.weight.weightKg} kg (${minimum.patient.name}, " +
                    "${DateFormatting.formatHumanDate(minimum.weight.date)}), maximum ${maximum.weight.weightKg} kg " +
                    "(${maximum.patient.name}, ${DateFormatting.formatHumanDate(maximum.weight.date)}).",
            )
            appendLine("WEIGHT DETAILS:")
            details.forEach(::appendLine)
            if (omitted > 0) {
                appendLine("- WEIGHT DETAILS TRUNCATED: $omitted more patients are included in the totals above.")
            }
        }.trimEnd()
    }

    private fun weightTrendLine(
        patient: Patient,
        series: List<Weight>,
    ): String {
        val ordered = series.sortedBy(Weight::date)
        val latest = ordered.last()
        if (ordered.size == 1) {
            return "- Weight ${patient.name}: single measurement ${latest.weightKg} kg " +
                "on ${DateFormatting.formatHumanDate(latest.date)}."
        }
        val previous = ordered[ordered.lastIndex - 1]
        val min = ordered.minBy(Weight::weightKg)
        val max = ordered.maxBy(Weight::weightKg)
        val direction = weightDirection(latest.weightKg, previous.weightKg)
        return "- Weight ${patient.name}: min ${min.weightKg} kg (${min.date}), max ${max.weightKg} kg " +
            "(${max.date}), latest ${latest.weightKg} kg (${latest.date}) - $direction."
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    /** |delta| at or below this reads as stable between the two latest weights. */
    private fun weightDirection(
        latestKg: Double,
        previousKg: Double,
    ): String =
        when {
            latestKg > previousKg + STABLE_WEIGHT_DELTA_KG -> "gaining"
            latestKg < previousKg - STABLE_WEIGHT_DELTA_KG -> "losing"
            else -> "stable"
        }

    /** Full-dataset vaccination/deworming/farrier totals with bounded detail lines. */
    private fun careBlock(
        targets: List<Patient>,
        dateRange: RagDateRange?,
    ): String? {
        if (targets.isEmpty()) return null
        val summaries = targets.map { patient -> careSummaryForPatient(patient, dateRange) }
        val withRecords = summaries.filter(CarePatientSummary::hasRecords)
        if (withRecords.isEmpty()) return "CARE COUNTS: no care records found for the selected patients."
        val lines = withRecords.take(MAX_PATIENTS_SCANNED).map(::careLineForPatient)
        val omitted = withRecords.size - lines.size
        val vaccinationCount = withRecords.sumOf { it.vaccinations.size }
        val dewormingCount = withRecords.sumOf { it.dewormings.size }
        val farrierCount = withRecords.sumOf { it.farrierVisits.size }
        return buildString {
            appendLine(
                "CARE TOTALS: $vaccinationCount vaccinations, $dewormingCount dewormings, " +
                    "$farrierCount farrier visits across ${withRecords.size} patients with records.",
            )
            appendLine("CARE COUNTS:")
            lines.forEach(::appendLine)
            if (omitted > 0) {
                appendLine("- CARE DETAILS TRUNCATED: $omitted more patients are included in the totals above.")
            }
        }.trimEnd()
    }

    private fun careSummaryForPatient(
        patient: Patient,
        dateRange: RagDateRange?,
    ): CarePatientSummary =
        CarePatientSummary(
            patient = patient,
            vaccinations =
                vaccinationRepository
                    .getByPatient(patient.id)
                    .filter { vaccination -> dateRange?.contains(vaccination.dateAdministered) != false },
            dewormings =
                dewormingRepository
                    .getByPatient(patient.id)
                    .filter { deworming -> dateRange?.contains(deworming.dateAdministered) != false },
            farrierVisits =
                farrierVisitRepository
                    .getByPatient(patient.id)
                    .filter { farrierVisit -> dateRange?.contains(farrierVisit.date) != false },
        )

    /** One patient's care line; callers filter out patients without records. */
    private fun careLineForPatient(summary: CarePatientSummary): String {
        val parts =
            listOf(
                carePart(
                    summary.vaccinations.size,
                    "vaccinations",
                    summary.vaccinations.maxOfOrNull { it.dateAdministered },
                ),
                carePart(
                    summary.dewormings.size,
                    "dewormings",
                    summary.dewormings.maxOfOrNull { it.dateAdministered },
                ),
                carePart(summary.farrierVisits.size, "farrier visits", summary.farrierVisits.maxOfOrNull { it.date }),
            )
        return "- Care ${summary.patient.name}: ${parts.joinToString(", ")}."
    }

    private fun carePart(
        count: Int,
        label: String,
        lastDate: LocalDate?,
    ): String = "$count $label" + (lastDate?.let { " (last ${DateFormatting.formatHumanDate(it)})" } ?: "")

    /** Active gestations with freshly computed day counts and foaling dates. */
    private fun gestationBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val rows =
            patients
                .flatMap { patient ->
                    gestationRepository
                        .getByPatient(patient.id)
                        .filter { it.isActiveGestation() }
                        .map { gestation -> GestationSummaryRow(patient, gestation) }
                }
        if (rows.isEmpty()) {
            return if (patients.isEmpty()) {
                null
            } else {
                "GESTATIONS: no active pregnancies found for the selected patients."
            }
        }
        val visible = rows.take(MAX_PATIENTS_SCANNED)
        val omitted = rows.size - visible.size
        val patientCount = rows.map { it.patient.id }.distinct().size
        return buildString {
            appendLine(
                "GESTATION TOTALS: ${rows.size} active ${pluralize("pregnancy", rows.size)} " +
                    "across $patientCount ${pluralize("patient", patientCount)}.",
            )
            appendLine("GESTATIONS:")
            visible.forEach { row -> appendLine(gestationLine(row.patient, row.gestation, today)) }
            if (omitted > 0) {
                appendLine("- GESTATION DETAILS TRUNCATED: $omitted more pregnancies are included in the total above.")
            }
        }.trimEnd()
    }

    /** Day count computed from breedingDate, never from the stored stale field. */
    private fun gestationLine(
        patient: Patient,
        gestation: Gestation,
        today: LocalDate,
    ): String {
        val progress = calculateGestationUseCase(gestation.breedingDate, today)
        return "- Gestation ${patient.name}: bred ${DateFormatting.formatHumanDate(gestation.breedingDate)}, " +
            "day ${progress.gestationDays}, " +
            "status ${gestation.status}, expected foaling ${progress.expectedDueDate}."
    }

    /** Care items whose next-due date already passed (due < today, strict). */
    private fun overdueBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val allLines =
            patients
                .flatMap { overdueLinesForPatient(it, today) }
        if (allLines.isEmpty()) {
            return if (patients.isEmpty()) null else "OVERDUE CARE (due before $today): no overdue care found."
        }
        val lines = allLines.take(MAX_OVERDUE_ITEMS)
        return buildString {
            appendLine("OVERDUE CARE (due before $today):")
            appendLine(lines.joinToString("\n"))
            val omitted = allLines.size - lines.size
            if (omitted > 0) {
                appendLine("- OVERDUE DETAILS TRUNCATED: $omitted more overdue items are included in the total above.")
            }
        }.trimEnd()
    }

    private fun overdueLinesForPatient(
        patient: Patient,
        today: LocalDate,
    ): List<String> =
        buildList {
            vaccinationRepository.getByPatient(patient.id).forEach { vaccination ->
                vaccination.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Vaccination ${vaccination.vaccineName} was due $due.")
                }
            }
            dewormingRepository.getByPatient(patient.id).forEach { deworming ->
                deworming.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Deworming ${deworming.product} was due $due.")
                }
            }
            farrierVisitRepository.getByPatient(patient.id).forEach { visit ->
                visit.nextDueDate?.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Farrier visit was due $due.")
                }
            }
            gestationRepository.getByPatient(patient.id).filter { it.isActiveGestation() }.forEach { gestation ->
                val dueDate = calculateGestationUseCase(gestation.breedingDate, today).expectedDueDate
                dueDate.takeIf { it < today }?.let { due ->
                    add("- OVERDUE ${patient.name}: Expected foaling was due $due.")
                }
            }
        }

    /**
     * Joins blocks under the whole-summary token cap. The first block always
     * fits (blocks are individually bounded by their own caps); later blocks
     * are dropped once the budget is spent.
     */
    private fun assemble(blocks: List<String>): String? {
        if (blocks.isEmpty()) return null
        var used = TokenEstimator.estimateTokensWithOverhead(SUMMARY_HEADER)
        return buildString {
            appendLine(SUMMARY_HEADER)
            for ((index, block) in blocks.withIndex()) {
                val cost = TokenEstimator.estimateTokensWithOverhead(block)
                if (index > 0 && used + cost > MAX_SUMMARY_TOKENS) break
                appendLine(block)
                used += cost
            }
        }.trimEnd()
    }

    internal companion object {
        /** Header prepended to every summary so the model recognizes the block. */
        const val SUMMARY_HEADER = "DETERMINISTIC SUMMARY (computed from database - authoritative):"

        /** Whole-summary cap (~400 tokens) inside the shared prompt budget. */
        private const val MAX_SUMMARY_TOKENS = 400

        /** Name-prefix tokens shorter than this never scope to a patient. */
        private const val MIN_NAME_PREFIX_CHARS = 2

        private const val MAX_NAMES_IN_CENSUS = 20
        private const val MAX_PATIENTS_SCANNED = 10
        private const val MAX_OVERDUE_ITEMS = 12

        private const val STABLE_WEIGHT_DELTA_KG = 0.5
    }
}

private fun pluralize(
    word: String,
    count: Int,
): String = if (count == 1) word else "${word}s"

/** True when the pregnancy has ended (foaled or failed): nothing active to report. */
private fun Gestation.isResolved(): Boolean =
    status.equals(RESOLVED_STATUS_COMPLETED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FAILED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FOALED, ignoreCase = true)

/** True only for a pregnancy that should still contribute current progress. */
private fun Gestation.isActiveGestation(): Boolean = isActive && !isResolved()

/** Matches the event types offered by the reproduction-card editor. */
private fun ReproductionEvent.isBreedingEvent(): Boolean {
    val normalized = eventType.trim().lowercase()
    return normalized == "breeding" ||
        normalized == "mating" ||
        normalized == "insemination" ||
        normalized == "cobertura" ||
        normalized == "cobrição" ||
        normalized == "cobricao"
}

// Same resolved-status vocabulary as GetUpcomingRemindersUseCase: foaled
// ("Completed") or failed pregnancies are not active gestations.
private const val RESOLVED_STATUS_COMPLETED = "Completed"
private const val RESOLVED_STATUS_FAILED = "Failed"
private const val RESOLVED_STATUS_FOALED = "Foaled"

/**
 * Deterministic intent detection for analysis-mode summaries. Conservative on
 * purpose: only count/list/aggregate phrasings trigger the repository scan -
 * ordinary retrieval questions must not pay the extra context cost. English
 * and Portuguese phrasings are covered because the assistant mirrors the
 * user's language per turn.
 */
@Suppress("TooManyFunctions") // One cohesive classifier exposes each supported analysis intent.
object AnalysisIntents {
    private val analysisRegex =
        Regex(
            "\\b(how many|how much has|how much have|average|trend|when was the last|which patients|total)\\b|" +
                "\\b(?:what|which)\\s+(?:patients?|horses?|mares?)\\s+do\\s+(?:i|we)\\s+have\\b|" +
                "\\bdo\\s+(?:i|we)\\s+have\\s+(?:any\\s+)?(?:patients?|horses?|mares?)\\b|" +
                "\\b(?:list|show)\\s+(?:my|our|the)\\s+(?:patients?|horses?|mares?)\\b|" +
                "\\b(quantos|quantas|quanto|média|media|tendência|tendencia|" +
                "quando foi a última|quando foi a ultima|quais pacientes|" +
                "(?:que|quais)\\s+(?:pacientes?|cavalos?|éguas?|eguas?)\\s+(?:tenho|temos)|" +
                "(?:mostra|liste|lista)\\s+(?:os|as)?\\s*(?:meus|minhas|nossos|nossas)?\\s*" +
                "(?:pacientes?|cavalos?|éguas?|eguas?))\\b",
        )

    private val censusRegex = Regex("\\b(patients|horses|pacientes|cavalos|égua|éguas)\\b")
    private val weightRegex =
        Regex(
            "\\b(weight|weights|weigh|weighs|weighed|weighing|peso|pesos|pesa|pesam|" +
                "pesada|pesado|pesagem)\\b",
        )

    private val careRegex =
        Regex(
            "\\b(vaccinations?|vaccines?|boosters?|dewormings?|dewormed|dewormer|farriers?|shod|shoeing|trims?|" +
                "vacinações?|vacinacoes?|vacinas?|desparasitações?|desparasitacoes?|" +
                "ferrageamentos?|ferrador|ferragem|care|cuidados?)\\b",
        )

    private val lastDoneRegex =
        Regex(
            "\\b(when was the last|quando foi a última|quando foi a ultima|" +
                "qual foi a última|qual foi a ultima|qual foi o último|qual foi o ultimo)\\b",
        )

    private val gestationRegex =
        Regex(
            "\\b(pregnant|gestations?|foaling|in foal|bred|breeding|prenha|prenhe|prenhes|" +
                "prenhez|gravidez|gestação|gestacao|gestações|gestacoes|parição|" +
                "paricao|parições|paricoes)\\b",
        )

    private val overdueRegex =
        Regex("\\b(overdue|due|upcoming|reminders?|atrasad[oa]s?|pendentes?|vencid[oa]s?)\\b")

    private val datasetReferenceRegex =
        Regex(
            "\\b(my|our|your)\\s+(patients?|horses?|mares?|records?|data|dataset|" +
                "weights?|vaccinations?|gestations?|care|history|timeline)\\b|" +
                "\\b(in|from|across|between|within)\\s+(?:my|our|the)\\s+" +
                "(records?|data|dataset|patients?|horses?|mares?|history|timeline)\\b|" +
                "\\b(?:meus|minhas|nossos|nossas)\\s+(pacientes?|cavalos?|éguas?|eguas?)\\b|" +
                "\\b(?:nos|nas)\\s+(?:meus|minhas|nossos|nossas)\\s+" +
                "(registos?|dados|pacientes?|cavalos?|éguas?|eguas?)\\b|" +
                "\\b(records?|dataset|data|statistics?|statistical|analysis|" +
                "compare|comparison|correlation|distribution|regression|outliers?|" +
                "by\\s+month|per\\s+month|over\\s+time|por\\s+m[eê]s|" +
                "por\\s+raça|por\\s+esp[eé]cie|dados|estatística|estatistica)\\b",
        )
    private val recordAnalysisTopicRegex =
        Regex(
            "\\b(overdue|upcoming|reminders?|" +
                "assistência|assistencia|atrasad[oa]s?|pendentes?|vencid[oa]s?)\\b",
        )
    private val careAnalysisReferenceRegex =
        Regex(
            "\\b(care|cuidados?)\\s+(?:records?|data|dataset|history|timeline|" +
                "registos?|dados|histórico|historico)\\b",
        )

    private val currentGestationRegex =
        Regex(
            "\\b(pregnant|pregnancy|pregnancies|in\\s+foal|days?\\s+along|gestation\\s+day|" +
                "due\\s+date|expected\\s+foaling|foaling\\s+date|pregnancy\\s+status|" +
                "current(?:ly)?\\s+(?:pregnan|pregnancy|gestation)|" +
                "prenha|prenhe|prenhes|prenhez|dia[s]?\\s+de\\s+gestação|dia[s]?\\s+de\\s+gestacao|" +
                "parto\\s+previsto|data\\s+do\\s+parto|parição|paricao)\\b",
        )

    private val breedingOutcomeRegex =
        Regex(
            "\\b(breeding\\s+(?:outcome|result)|outcome\\s+of\\s+(?:the\\s+)?breeding|" +
                "resultado\\s+(?:da\\s+)?cobertura|resultado\\s+reprodutivo|" +
                "desfecho\\s+(?:da\\s+)?cobertura)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val breedingTimingRegex =
        Regex(
            "\\b(bred|breeding\\s+date|date\\s+(?:was\\s+)?bred|" +
                "when\\s+was\\s+[^?]+\\s+bred|how\\s+long\\s+ago\\s+[^?]+\\s+bred|" +
                "coberta|data\\s+da\\s+cobertura|quando\\s+foi\\s+coberta|" +
                "há\\s+quanto\\s+tempo\\s+[^?]+\\s+coberta)\\b",
        )

    /**
     * Questions that benefit from the cloud's larger context and native tools
     * instead of a single compact summary. This is deliberately conservative:
     * simple patient lookups remain eligible for the on-device path.
     */
    private val toolAnalysisRegex =
        Regex(
            "\\b(analy[sz]e|analysis|dataset|statistics?|statistical|average|mean|trend|" +
                "compare|comparison|correlat|" +
                "percentage|proportion|distribution|median|variance|regression|outlier|" +
                "across|between|by month|per month|by breed|by species|over time|pattern|relationship|" +
                "média|media|analisar|análise|analise|dados|estatística|estatistica|comparar|" +
                "correlação|correlacao|percentagem|proporção|proporcao|distribuição|" +
                "mediana|variância|variancia|tendência|tendencia|padrão|padrao|relação|relacao|" +
                "por mês|por mes|por raça|por raca|por espécie|por especie)\\b",
        )

    /**
     * True when [query] asks a count/list/aggregate question at all: either
     * an explicit analysis pattern ("how many", "trend") or any analysis
     * topic word (weight/vaccination/pregnancy/overdue), because status
     * questions like "Is Bella pregnant?" deserve the computed block too.
     */
    fun isAnalysisQuery(query: String): Boolean {
        val lowered = query.lowercase()
        val isGeneralQuestion = RecordQuestionIntent.isGeneralKnowledgeQuestion(query)
        val hasScope = hasRecordAnalysisScope(query, lowered)
        return !isGeneralQuestion &&
            hasScope &&
            (analysisRegex.containsMatchIn(lowered) || hasAnalysisTopic(lowered))
    }

    /** Census block: explicit patient/horse listing, or a generic count with no topic. */
    fun wantsCensus(query: String): Boolean {
        val lowered = query.lowercase()
        if (censusRegex.containsMatchIn(lowered)) return true
        return !hasAnalysisTopic(lowered)
    }

    fun wantsWeight(query: String): Boolean = weightRegex.containsMatchIn(query.lowercase())

    fun wantsCareCounts(query: String): Boolean {
        val lowered = query.lowercase()
        return careRegex.containsMatchIn(lowered) || lastDoneRegex.containsMatchIn(lowered)
    }

    fun wantsGestation(query: String): Boolean = gestationRegex.containsMatchIn(query.lowercase())

    /** True for status/day/due-date/breeding-timing questions needing live gestation facts. */
    fun wantsCurrentGestation(query: String): Boolean {
        val lowered = query.lowercase()
        return currentGestationRegex.containsMatchIn(lowered) ||
            breedingTimingRegex.containsMatchIn(lowered) ||
            breedingOutcomeRegex.containsMatchIn(lowered)
    }

    /** True when the user asks when a recorded breeding happened or how long ago it was. */
    fun wantsBreedingTiming(query: String): Boolean = breedingTimingRegex.containsMatchIn(query.lowercase())

    /** True when the user asks for the recorded result of a breeding cycle. */
    fun wantsBreedingOutcome(query: String): Boolean = breedingOutcomeRegex.containsMatchIn(query.lowercase())

    fun wantsOverdue(query: String): Boolean = overdueRegex.containsMatchIn(query.lowercase())

    /** True when the question asks for a broader, multi-record analysis pass. */
    fun requiresTools(query: String): Boolean =
        isAnalysisQuery(query) &&
            toolAnalysisRegex.containsMatchIn(query.lowercase())

    private fun hasRecordAnalysisScope(
        query: String,
        lowered: String,
    ): Boolean =
        datasetReferenceRegex.containsMatchIn(lowered) ||
            RecordQuestionIntent.hasIndividualPatientReference(query) ||
            RecordQuestionIntent.hasLikelyNamedPatientReference(query) ||
            RecordQuestionIntent.hasGestationPopulationReference(query) ||
            RecordQuestionIntent.isRecordQuestion(query, null, null) ||
            recordAnalysisTopicRegex.containsMatchIn(lowered) ||
            careAnalysisReferenceRegex.containsMatchIn(lowered)
}

private fun hasAnalysisTopic(lowered: String): Boolean =
    AnalysisIntents.wantsWeight(lowered) ||
        AnalysisIntents.wantsCareCounts(lowered) ||
        AnalysisIntents.wantsGestation(lowered) ||
        AnalysisIntents.wantsOverdue(lowered)
