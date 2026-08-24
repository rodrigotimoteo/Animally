package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient

/**
 * Outcome of resolving a spoken patient name against the patient list.
 */
sealed interface PatientResolution {
    /** Exactly one active patient matched the normalized name. */
    data class Resolved(
        val patient: Patient,
    ) : PatientResolution

    /** Several active patients share the normalized name. */
    data class Ambiguous(
        val candidates: List<Patient>,
    ) : PatientResolution

    /** No active patient matched the normalized name. */
    data object NotFound : PatientResolution
}

/**
 * Resolves a patient name from a dictation transcript to existing patients.
 *
 * Matching is exact on the fully normalized name only — no fuzzy or prefix
 * matching. Normalization lowercases the name and strips Portuguese
 * diacritics via a hardcoded map (commonMain has no `java.text.Normalizer`),
 * so "Trovão" matches "Trovao".
 *
 * @param patientRepository Repository instance for accessing patients.
 */
class ResolvePatientUseCase(
    private val patientRepository: IPatientRepository,
) {
    /**
     * Resolves [name] against all active patients.
     *
     * @param name Patient name as captured in the transcript.
     * @return [PatientResolution.Resolved] on a single exact match,
     *   [PatientResolution.Ambiguous] when several patients match, and
     *   [PatientResolution.NotFound] when none does.
     */
    operator fun invoke(name: String): PatientResolution {
        val normalized = normalize(name)
        if (normalized.isEmpty()) return PatientResolution.NotFound
        val candidates =
            patientRepository.getPatientList().filter {
                normalize(it.name) == normalized
            }
        return when (candidates.size) {
            0 -> PatientResolution.NotFound
            1 -> PatientResolution.Resolved(candidates.single())
            else -> PatientResolution.Ambiguous(candidates)
        }
    }

    private companion object {
        val DIACRITICS: Map<Char, Char> =
            buildMap {
                putAll("áàâãä".map { it to 'a' })
                putAll("éèêë".map { it to 'e' })
                putAll("íìîï".map { it to 'i' })
                putAll("óòôõö".map { it to 'o' })
                putAll("úùûü".map { it to 'u' })
                put('ç', 'c')
                put('ñ', 'n')
            }

        fun normalize(name: String): String {
            val lowered = name.trim().lowercase()
            return lowered.map { DIACRITICS[it] ?: it }.joinToString("")
        }
    }
}
