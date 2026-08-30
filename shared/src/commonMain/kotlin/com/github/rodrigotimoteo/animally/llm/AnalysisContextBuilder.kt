package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GestationProgress
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.llm.analysis.AnalysisAssembler
import com.github.rodrigotimoteo.animally.llm.analysis.AnalysisIntents
import com.github.rodrigotimoteo.animally.llm.analysis.BreedingFactsProvider
import com.github.rodrigotimoteo.animally.llm.analysis.CareSummaryBuilder
import com.github.rodrigotimoteo.animally.llm.analysis.GestationFactsProvider
import com.github.rodrigotimoteo.animally.llm.analysis.GestationSummaryBuilder
import com.github.rodrigotimoteo.animally.llm.analysis.OverdueCareBuilder
import com.github.rodrigotimoteo.animally.llm.analysis.PatientCensusBuilder
import com.github.rodrigotimoteo.animally.llm.analysis.PatientScopeResolver
import com.github.rodrigotimoteo.animally.llm.analysis.WeightSummaryBuilder
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
 * model NARRATES. Facade delegates per-domain assembly to small builders in
 * `llm.analysis` so each domain stays <200 lines while the public API for
 * GenerateRagResponseUseCase / AnalysisToolRegistry remains stable.
 */
@Suppress("TooManyFunctions")
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
    private val scopeResolver = PatientScopeResolver()
    private val censusBuilder = PatientCensusBuilder()
    private val weightBuilder = WeightSummaryBuilder(weightRepository)
    private val careBuilder =
        CareSummaryBuilder(
            vaccinationRepository = vaccinationRepository,
            dewormingRepository = dewormingRepository,
            farrierVisitRepository = farrierVisitRepository,
        )
    private val gestationBuilder =
        GestationSummaryBuilder(
            gestationRepository = gestationRepository,
            calculateGestationUseCase = calculateGestationUseCase,
        )
    private val overdueBuilder =
        OverdueCareBuilder(
            vaccinationRepository = vaccinationRepository,
            dewormingRepository = dewormingRepository,
            farrierVisitRepository = farrierVisitRepository,
            gestationRepository = gestationRepository,
            calculateGestationUseCase = calculateGestationUseCase,
        )
    private val gestationFactsProvider =
        GestationFactsProvider(
            patientRepository = patientRepository,
            gestationRepository = gestationRepository,
            calculateGestationUseCase = calculateGestationUseCase,
            scopeResolver = scopeResolver,
        )
    private val breedingFactsProvider =
        BreedingFactsProvider(
            patientRepository = patientRepository,
            reproductionRepository = reproductionRepository,
            calculateGestationUseCase = calculateGestationUseCase,
            scopeResolver = scopeResolver,
        )

    fun build(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): String? {
        if (!AnalysisIntents.isAnalysisQuery(query)) return null
        val patients = patientRepository.getPatientList()
        val matchedPatients = scopeResolver.patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val dateRange = RagDateRangeIntent.resolve(query, today)
        val hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query)
        val hasLikelyName = RecordQuestionIntent.hasLikelyNamedPatientReference(query)
        val careTargets =
            scopeResolver.resolveCareTargets(
                patients,
                matchedPatients,
                scoped,
                hasIndividualReference,
                hasLikelyName,
            )
        val blocks =
            buildList {
                if (AnalysisIntents.wantsCensus(query)) add(censusBuilder.censusBlock(patients))
                if (AnalysisIntents.wantsWeight(query)) {
                    weightBuilder.weightTrendBlock(careTargets, dateRange)?.let(::add)
                }
                if (AnalysisIntents.wantsCareCounts(query)) {
                    careBuilder.careBlock(careTargets, dateRange)?.let(::add)
                }
                if (AnalysisIntents.wantsGestation(query)) {
                    gestationBuilder.gestationBlock(careTargets, today)?.let(::add)
                }
                if (AnalysisIntents.wantsOverdue(query)) overdueBuilder.overdueBlock(careTargets, today)?.let(::add)
            }
        return AnalysisAssembler.assemble(blocks)
    }

    internal fun gestationFacts(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<GestationFact>? = gestationFactsProvider.gestationFacts(query, today)

    internal fun breedingFacts(
        query: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<BreedingFact>? = breedingFactsProvider.breedingFacts(query, today)

    @Suppress("MaxLineLength")
    internal fun reproductionOutcomeFacts(query: String): List<BreedingOutcomeFact>? = breedingFactsProvider.reproductionOutcomeFacts(query)

    @Suppress("MaxLineLength")
    internal fun reproductionAttributeFacts(query: String): List<ReproductionAttributeFact>? = breedingFactsProvider.reproductionAttributeFacts(query)

    internal companion object {
        const val SUMMARY_HEADER = com.github.rodrigotimoteo.animally.llm.analysis.SUMMARY_HEADER
    }
}
