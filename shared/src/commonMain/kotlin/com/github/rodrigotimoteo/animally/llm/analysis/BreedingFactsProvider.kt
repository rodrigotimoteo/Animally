package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.llm.BreedingFact
import com.github.rodrigotimoteo.animally.llm.BreedingOutcomeFact
import com.github.rodrigotimoteo.animally.llm.RecordQuestionIntent
import com.github.rodrigotimoteo.animally.llm.ReproductionAttributeFact
import com.github.rodrigotimoteo.animally.llm.ReproductionAttributeIntent

internal class BreedingFactsProvider(
    private val patientRepository: IPatientRepository,
    private val reproductionRepository: IReproductionRepository?,
    private val calculateGestationUseCase: CalculateGestationUseCase,
    private val scopeResolver: PatientScopeResolver,
) {
    fun breedingFacts(
        query: String,
        today: kotlinx.datetime.LocalDate,
    ): List<BreedingFact>? {
        if (!AnalysisTopicIntents.wantsBreedingTiming(query)) return null
        val repository = reproductionRepository ?: return emptyList()
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

    fun reproductionOutcomeFacts(query: String): List<BreedingOutcomeFact>? {
        if (!AnalysisTopicIntents.wantsBreedingOutcome(query)) return null
        val repository = reproductionRepository ?: return null
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
                repository.getByPatient(patient.id).map { event -> BreedingOutcomeFact(patient, event) }
            }.sortedWith(compareBy<BreedingOutcomeFact> { it.event.date }.thenBy { it.patient.name.lowercase() })
    }

    fun reproductionAttributeFacts(query: String): List<ReproductionAttributeFact>? {
        if (ReproductionAttributeIntent.requestedAttribute(query) == null) return null
        val repository = reproductionRepository ?: return emptyList()
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
                repository
                    .getByPatient(patient.id)
                    .filter { event -> event.isActive && event.isBreedingEvent() }
                    .map { event -> ReproductionAttributeFact(patient, event) }
            }.sortedWith(
                compareByDescending<ReproductionAttributeFact> { it.event.date }
                    .thenBy { it.patient.name.lowercase() },
            )
    }
}
