package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.llm.support.SharedStopWords

internal class PatientScopeResolver {
    fun patientNameMatches(
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

    fun resolveCareTargets(
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
}
