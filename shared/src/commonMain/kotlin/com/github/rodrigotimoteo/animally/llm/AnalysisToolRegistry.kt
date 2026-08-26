package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Safe, read-only analysis tools backed by shared repositories. The model may
 * choose a tool, but it cannot choose SQL, access a repository directly, or
 * write application data.
 */
class AnalysisToolRegistry(
    patientRepository: IPatientRepository,
    weightRepository: IWeightRepository,
    vaccinationRepository: IVaccinationRepository,
    dewormingRepository: IDewormingRepository,
    farrierVisitRepository: IFarrierVisitRepository,
    gestationRepository: IGestationRepository,
    calculateGestationUseCase: CalculateGestationUseCase,
    todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : RagToolRegistry {
    private val input = AnalysisToolSupport(patientRepository)
    private val censusTool = PatientCensusTool(patientRepository, input)
    private val weightTool = WeightSummaryTool(weightRepository, input, todayProvider)
    private val careTool =
        CareSummaryTool(
            vaccinationRepository = vaccinationRepository,
            dewormingRepository = dewormingRepository,
            farrierVisitRepository = farrierVisitRepository,
            input = input,
            todayProvider = todayProvider,
        )
    private val gestationTool =
        GestationSummaryTool(
            gestationRepository = gestationRepository,
            calculateGestationUseCase = calculateGestationUseCase,
            input = input,
            todayProvider = todayProvider,
        )

    override val definitions: List<RagToolDefinition> = AnalysisToolSchemas.definitions

    override suspend fun execute(call: RagToolCall): RagToolResult =
        try {
            when (call.name) {
                AnalysisToolNames.PATIENT_CENSUS -> censusTool.execute(call)
                AnalysisToolNames.WEIGHT_SUMMARY -> weightTool.execute(call)
                AnalysisToolNames.CARE_SUMMARY -> careTool.execute(call)
                AnalysisToolNames.GESTATION_SUMMARY -> gestationTool.execute(call)
                else -> throw AnalysisToolInputException("Unknown analysis tool: ${call.name}.")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            RagToolResult(
                toolCallId = call.id,
                name = call.name,
                content = analysisErrorContent(t.message ?: "The tool could not complete."),
                isError = true,
            )
        }
}
