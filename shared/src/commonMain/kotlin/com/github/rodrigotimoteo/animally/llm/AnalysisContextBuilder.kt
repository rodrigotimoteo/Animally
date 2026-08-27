package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GestationProgress
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Database-backed gestation facts projected for a single assistant turn. */
internal data class GestationFact(
    val patient: Patient,
    val gestation: Gestation,
    val progress: GestationProgress,
    val isActive: Boolean,
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
    private val calculateGestationUseCase: CalculateGestationUseCase = CalculateGestationUseCase(),
) {
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
                if (scoped != null && AnalysisIntents.wantsWeight(query)) weightTrendBlock(scoped)?.let(::add)
                if (AnalysisIntents.wantsCareCounts(query)) careBlock(careTargets)?.let(::add)
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
            .take(MAX_PATIENTS_SCANNED)
            .flatMap { patient ->
                gestationRepository.getByPatient(patient.id).map { gestation ->
                    val active = gestation.isActiveGestation()
                    GestationFact(
                        patient = patient,
                        gestation = gestation,
                        progress =
                            if (active) {
                                calculateGestationUseCase(gestation.breedingDate, today)
                            } else {
                                GestationProgress(gestation.expectedDueDate, gestation.gestationDays)
                            },
                        isActive = active,
                    )
                }
            }.sortedWith(compareBy({ !it.isActive }, { it.gestation.breedingDate }, { it.patient.name.lowercase() }))
    }

    /**
     * Patients whose names prefix-match a query token (case-insensitive,
     * possessives stripped). The caller treats multiple matches as ambiguous
     * so summaries and retrieval agree on which patient the question is about.
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
                }.filter { it.length >= MIN_NAME_PREFIX_CHARS && it.lowercase() !in PATIENT_SCOPE_STOP_WORDS }
                .map(String::lowercase)
                .toSet()
        if (tokens.isEmpty()) return emptyList()
        return patients
            .filter { patient -> tokens.any { token -> patient.name.lowercase().startsWith(token) } }
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
        return "PATIENT CENSUS: ${patients.size} active patients: $names$overflow."
    }

    /**
     * Weight trend for one patient: series min/max/latest with dates and a
     * direction derived from the two most recent measurements. A single
     * entry has no trend, so it is reported as one measurement instead of
     * inventing min == max == latest noise.
     */
    private fun weightTrendBlock(patient: Patient): String? {
        val series = weightRepository.getByPatient(patient.id).sortedBy(Weight::date)
        val latest = series.lastOrNull() ?: return null
        if (series.size == 1) {
            return "- Weight ${patient.name}: single measurement ${latest.weightKg} kg on ${latest.date}."
        }
        val previous = series[series.lastIndex - 1]
        val min = series.minBy(Weight::weightKg)
        val max = series.maxBy(Weight::weightKg)
        val direction = weightDirection(latest.weightKg, previous.weightKg)
        return "- Weight ${patient.name}: min ${min.weightKg} kg (${min.date}), max ${max.weightKg} kg " +
            "(${max.date}), latest ${latest.weightKg} kg (${latest.date}) - $direction."
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

    /** Per-patient vaccination/deworming/farrier counts with last-done dates. */
    private fun careBlock(targets: List<Patient>): String? {
        val lines = targets.take(MAX_PATIENTS_SCANNED).mapNotNull(::careLineForPatient)
        if (lines.isEmpty()) return null
        return buildString {
            appendLine("CARE COUNTS:")
            appendLine(lines.joinToString("\n"))
        }.trimEnd()
    }

    /** One patient's care line, or null when the patient has no care records. */
    private fun careLineForPatient(patient: Patient): String? {
        val vaccinations = vaccinationRepository.getByPatient(patient.id)
        val dewormings = dewormingRepository.getByPatient(patient.id)
        val farrierVisits = farrierVisitRepository.getByPatient(patient.id)
        if (vaccinations.isEmpty() && dewormings.isEmpty() && farrierVisits.isEmpty()) return null
        val parts =
            listOf(
                carePart(vaccinations.size, "vaccinations", vaccinations.maxOfOrNull { it.dateAdministered }),
                carePart(dewormings.size, "dewormings", dewormings.maxOfOrNull { it.dateAdministered }),
                carePart(farrierVisits.size, "farrier visits", farrierVisits.maxOfOrNull { it.date }),
            )
        return "- Care ${patient.name}: ${parts.joinToString(", ")}."
    }

    private fun carePart(
        count: Int,
        label: String,
        lastDate: LocalDate?,
    ): String = "$count $label" + (lastDate?.let { " (last ${formatHumanDate(it)})" } ?: "")

    /** Active gestations with freshly computed day counts and foaling dates. */
    private fun gestationBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val lines =
            patients
                .take(MAX_PATIENTS_SCANNED)
                .flatMap { patient ->
                    gestationRepository
                        .getByPatient(patient.id)
                        .filter { it.isActiveGestation() }
                        .map { gestation -> gestationLine(patient, gestation, today) }
                }
        if (lines.isEmpty()) return null
        return buildString {
            appendLine("GESTATIONS:")
            appendLine(lines.joinToString("\n"))
        }.trimEnd()
    }

    /** Day count computed from breedingDate, never from the stored stale field. */
    private fun gestationLine(
        patient: Patient,
        gestation: Gestation,
        today: LocalDate,
    ): String {
        val progress = calculateGestationUseCase(gestation.breedingDate, today)
        return "- Gestation ${patient.name}: day ${progress.gestationDays}, " +
            "status ${gestation.status}, expected foaling ${progress.expectedDueDate}."
    }

    /** Care items whose next-due date already passed (due < today, strict). */
    private fun overdueBlock(
        patients: List<Patient>,
        today: LocalDate,
    ): String? {
        val lines =
            patients
                .take(MAX_PATIENTS_SCANNED)
                .flatMap { overdueLinesForPatient(it, today) }
                .take(MAX_OVERDUE_ITEMS)
        if (lines.isEmpty()) return null
        return buildString {
            appendLine("OVERDUE CARE (due before $today):")
            appendLine(lines.joinToString("\n"))
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
        var used = estimateTokens(SUMMARY_HEADER)
        return buildString {
            appendLine(SUMMARY_HEADER)
            for ((index, block) in blocks.withIndex()) {
                val cost = estimateTokens(block)
                if (index > 0 && used + cost > MAX_SUMMARY_TOKENS) break
                appendLine(block)
                used += cost
            }
        }.trimEnd()
    }

    private fun estimateTokens(text: String): Int = (text.length / CHARS_PER_TOKEN).toInt() + 1

    internal companion object {
        /** Header prepended to every summary so the model recognizes the block. */
        const val SUMMARY_HEADER = "DETERMINISTIC SUMMARY (computed from database - authoritative):"

        private const val CHARS_PER_TOKEN = 4.0

        /** Whole-summary cap (~400 tokens) inside the shared prompt budget. */
        private const val MAX_SUMMARY_TOKENS = 400

        /** Name-prefix tokens shorter than this never scope to a patient. */
        private const val MIN_NAME_PREFIX_CHARS = 2

        private const val MAX_NAMES_IN_CENSUS = 20
        private const val MAX_PATIENTS_SCANNED = 10
        private const val MAX_OVERDUE_ITEMS = 12

        private const val STABLE_WEIGHT_DELTA_KG = 0.5

        private val PATIENT_SCOPE_STOP_WORDS =
            setOf(
                "what",
                "when",
                "which",
                "who",
                "how",
                "why",
                "where",
                "did",
                "do",
                "does",
                "is",
                "are",
                "was",
                "were",
                "the",
                "a",
                "an",
                "of",
                "for",
                "to",
                "in",
                "on",
                "any",
                "have",
                "has",
                "had",
                "my",
                "our",
                "your",
                "this",
                "that",
                "patient",
                "patients",
                "horse",
                "horses",
                "mare",
                "mares",
                "cavalo",
                "cavalos",
                "égua",
                "éguas",
                "paciente",
                "pacientes",
                "o",
                "os",
                "as",
                "um",
                "uma",
                "uns",
                "umas",
                "que",
                "foi",
                "são",
                "sao",
                "não",
                "nao",
                "há",
                "ha",
                "do",
                "da",
                "dos",
                "das",
                "em",
                "com",
                "para",
                "por",
                "como",
                "porque",
                "porquê",
                "tenho",
                "temos",
                "está",
                "esta",
                "é",
                "e",
                "aconteceu",
                "ocorreu",
                "pregnant",
                "pregnancy",
                "gestation",
                "vaccination",
                "vaccinations",
                "vaccine",
                "farrier",
                "visit",
                "visits",
                "deworming",
                "weight",
                "ultrasound",
                "latest",
                "last",
                "previous",
                "recent",
                "record",
                "records",
                "treatment",
                "treatments",
                "month",
                "week",
                "year",
                "este",
                "esta",
                "neste",
                "nesta",
                "mês",
                "mes",
                "semana",
                "ano",
                "hoje",
                "ontem",
                "quando",
                "qual",
                "quais",
                "quantos",
                "quantas",
                "último",
                "última",
                "ultimo",
                "ultima",
                "recente",
                "recentes",
                "registo",
                "registos",
            )
    }
}

/** Human-readable month abbreviations for summary dates (locale-independent). */
private val MONTH_ABBREVIATIONS =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Renders a date as "24 Aug 2026" (locale-independent, model-friendly).
 * Raw ISO strings in the summary leak into spoken answers verbatim - the
 * model parrots exactly what the authoritative block shows. File-level so
 * the class stays under its detekt function-count threshold.
 */
private fun formatHumanDate(date: LocalDate): String {
    val month = MONTH_ABBREVIATIONS[date.month.ordinal]
    return "${date.day} $month ${date.year}"
}

/** True when the pregnancy has ended (foaled or failed): nothing active to report. */
private fun Gestation.isResolved(): Boolean =
    status.equals(RESOLVED_STATUS_COMPLETED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FAILED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FOALED, ignoreCase = true)

/** True only for a pregnancy that should still contribute current progress. */
private fun Gestation.isActiveGestation(): Boolean = isActive && !isResolved()

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
object AnalysisIntents {
    private val analysisRegex =
        Regex(
            "\\b(how many|how much has|how much have|average|trend|when was the last|which patients|total)\\b|" +
                "\\b(quantos|quantas|quanto|média|media|tendência|tendencia|" +
                "quando foi a última|quando foi a ultima|quais pacientes)\\b",
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
                "ferrageamentos?|ferrador|ferragem)\\b",
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

    private val currentGestationRegex =
        Regex(
            "\\b(pregnant|pregnancy|pregnancies|in\\s+foal|days?\\s+along|gestation\\s+day|" +
                "due\\s+date|expected\\s+foaling|foaling\\s+date|pregnancy\\s+status|" +
                "current(?:ly)?\\s+(?:pregnan|pregnancy|gestation)|" +
                "prenha|prenhe|prenhes|prenhez|dia[s]?\\s+de\\s+gestação|dia[s]?\\s+de\\s+gestacao|" +
                "parto\\s+previsto|data\\s+do\\s+parto|parição|paricao)\\b",
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
        if (analysisRegex.containsMatchIn(lowered)) return true
        return anyTopic(lowered)
    }

    /** Census block: explicit patient/horse listing, or a generic count with no topic. */
    fun wantsCensus(query: String): Boolean {
        val lowered = query.lowercase()
        if (censusRegex.containsMatchIn(lowered)) return true
        return !anyTopic(lowered)
    }

    fun wantsWeight(query: String): Boolean = weightRegex.containsMatchIn(query.lowercase())

    fun wantsCareCounts(query: String): Boolean {
        val lowered = query.lowercase()
        return careRegex.containsMatchIn(lowered) || lastDoneRegex.containsMatchIn(lowered)
    }

    fun wantsGestation(query: String): Boolean = gestationRegex.containsMatchIn(query.lowercase())

    /** True for status/day/due-date questions that need live gestation facts. */
    fun wantsCurrentGestation(query: String): Boolean = currentGestationRegex.containsMatchIn(query.lowercase())

    fun wantsOverdue(query: String): Boolean = overdueRegex.containsMatchIn(query.lowercase())

    /** True when the question asks for a broader, multi-record analysis pass. */
    fun requiresTools(query: String): Boolean = toolAnalysisRegex.containsMatchIn(query.lowercase())

    private fun anyTopic(lowered: String): Boolean =
        wantsWeight(lowered) ||
            wantsCareCounts(lowered) ||
            wantsGestation(lowered) ||
            wantsOverdue(lowered)
}
