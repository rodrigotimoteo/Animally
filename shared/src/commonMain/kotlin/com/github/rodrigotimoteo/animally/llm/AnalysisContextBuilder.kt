package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

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
class AnalysisContextBuilder(
    private val patientRepository: IPatientRepository,
    private val weightRepository: IWeightRepository,
    private val vaccinationRepository: IVaccinationRepository,
    private val dewormingRepository: IDewormingRepository,
    private val farrierVisitRepository: IFarrierVisitRepository,
    private val gestationRepository: IGestationRepository,
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
        val scoped = scopedPatient(patients, query)
        val careTargets = if (scoped != null) listOf(scoped) else patients
        val blocks =
            buildList {
                if (AnalysisIntents.wantsCensus(query)) add(censusBlock(patients))
                if (scoped != null && AnalysisIntents.wantsWeight(query)) weightTrendBlock(scoped)?.let(::add)
                if (AnalysisIntents.wantsCareCounts(query)) careBlock(careTargets)?.let(::add)
                if (AnalysisIntents.wantsGestation(query)) gestationBlock(patients, today)?.let(::add)
                if (AnalysisIntents.wantsOverdue(query)) overdueBlock(patients, today)?.let(::add)
            }
        return assemble(blocks)
    }

    /**
     * The single patient whose name prefix-matches a query token
     * (case-insensitive, possessives stripped), or null when ambiguous or
     * unmatched. Mirrors GenerateRagResponseUseCase's scoping so summaries
     * and retrieval agree on which patient the question is about.
     */
    private fun scopedPatient(
        patients: List<Patient>,
        query: String,
    ): Patient? {
        val tokens =
            query
                .split(Regex("\\s+"))
                .map { it.trim('?', ',', '.', '!', ':', ';', '\'').removeSuffix("'s") }
                .filter { it.length >= MIN_NAME_PREFIX_CHARS }
                .map(String::lowercase)
                .toSet()
        if (tokens.isEmpty()) return null
        return patients
            .filter { patient -> tokens.any { token -> patient.name.lowercase().startsWith(token) } }
            .singleOrNull()
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
    ): String = "$count $label" + (lastDate?.let { " (last $it)" } ?: "")

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
                        .filterNot(Gestation::isResolved)
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
    ): String =
        "- Gestation ${patient.name}: day ${gestation.breedingDate.daysUntil(today)}, " +
            "status ${gestation.status}, expected foaling ${gestation.expectedDueDate}."

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
            gestationRepository.getByPatient(patient.id).filterNot(Gestation::isResolved).forEach { gestation ->
                gestation.expectedDueDate.takeIf { it < today }?.let { due ->
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
    }
}

/** True when the pregnancy has ended (foaled or failed): nothing active to report. */
private fun Gestation.isResolved(): Boolean =
    status.equals(RESOLVED_STATUS_COMPLETED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FAILED, ignoreCase = true)

// Same resolved-status vocabulary as GetUpcomingRemindersUseCase: foaled
// ("Completed") or failed pregnancies are not active gestations.
private const val RESOLVED_STATUS_COMPLETED = "Completed"
private const val RESOLVED_STATUS_FAILED = "Failed"

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
                "\\b(quantos|quanto|média|tendência|tendencia|" +
                "quando foi a última|quando foi a ultima|quais pacientes)\\b",
        )

    private val censusRegex = Regex("\\b(patients|horses|pacientes|cavalos|égua|éguas)\\b")
    private val weightRegex = Regex("\\b(weight|weights|weigh|weighs|weighing|peso|pesa|pesam)\\b")

    private val careRegex =
        Regex(
            "\\b(vaccinations?|vaccines?|boosters?|dewormings?|dewormed|dewormer|farriers?|shod|shoeing|trims?|" +
                "vacinas?|desparasitações?|ferrageamentos?)\\b",
        )

    private val lastDoneRegex = Regex("\\b(when was the last|quando foi a última|quando foi a ultima)\\b")

    private val gestationRegex =
        Regex("\\b(pregnant|gestations?|foaling|in foal|bred|breeding|prenha|gestações?|parições?)\\b")

    private val overdueRegex =
        Regex("\\b(overdue|due|upcoming|reminders?|atrasad[oa]s?|pendentes?|vencid[oa]s?)\\b")

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

    fun wantsOverdue(query: String): Boolean = overdueRegex.containsMatchIn(query.lowercase())

    private fun anyTopic(lowered: String): Boolean =
        wantsWeight(lowered) ||
            wantsCareCounts(lowered) ||
            wantsGestation(lowered) ||
            wantsOverdue(lowered)
}
