package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.isActiveGestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GestationProgress
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.llm.GestationFact
import com.github.rodrigotimoteo.animally.llm.RecordQuestionIntent
import kotlinx.datetime.LocalDate

internal class GestationFactsProvider(
    private val patientRepository: IPatientRepository,
    private val gestationRepository: IGestationRepository,
    private val calculateGestationUseCase: CalculateGestationUseCase,
    private val scopeResolver: PatientScopeResolver,
) {
    fun gestationFacts(
        query: String,
        today: LocalDate,
    ): List<GestationFact>? {
        if (!AnalysisTopicIntents.wantsCurrentGestation(query)) return null
        val patients = patientRepository.getPatientList()
        val matchedPatients = scopeResolver.patientNameMatches(patients, query)
        val scoped = matchedPatients.singleOrNull()
        val careTargets =
            scopeResolver.resolveCareTargets(
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
}
