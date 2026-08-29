package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/**
 * Conservative intent detection for facts stored directly on a patient row.
 * Keeping this separate from record-type detection prevents a cloud model from
 * rewriting exact identity values such as a date of birth.
 */
internal object PatientIdentityAnswer {
    private val dateOfBirthRegex =
        Regex(
            "\\b(date\\s+of\\s+birth|birth\\s+date|dob|born|birthday|" +
                "data\\s+de\\s+nascimento|nasceu|aniversário|aniversario)\\b",
            RegexOption.IGNORE_CASE,
        )

    fun isDateOfBirthQuestion(query: String): Boolean = dateOfBirthRegex.containsMatchIn(query)
}

/**
 * Emits an exact date-of-birth projection for one resolved active patient.
 * A missing value is also answered deterministically so the cloud model cannot
 * turn an absent field into a plausible date.
 */
internal suspend fun FlowCollector<RagStreamEvent>.emitPatientDateOfBirthAnswer(
    query: String,
    scopedPatient: String?,
    patientRepository: IPatientRepository?,
): Boolean {
    if (!PatientIdentityAnswer.isDateOfBirthQuestion(query) || scopedPatient == null || patientRepository == null) {
        return false
    }
    val patient =
        patientRepository
            .getPatientList()
            .singleOrNull { it.name.equals(scopedPatient, ignoreCase = true) }
            ?: return false
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    val answer =
        when {
            patient.dateOfBirth != null && portuguese ->
                "A data de nascimento de ${patient.name} é ${formatHumanDateShort(patient.dateOfBirth)}."
            patient.dateOfBirth != null ->
                "${patient.name}'s date of birth is ${formatHumanDateShort(patient.dateOfBirth)}."
            portuguese -> "A data de nascimento de ${patient.name} não está registada."
            else -> "The date of birth for ${patient.name} is not recorded."
        }
    emit(RagStreamEvent.Chunk(answer))
    emit(RagStreamEvent.Sources(listOf(patientSource(patient))))
    return true
}

private fun patientSource(patient: Patient): SearchResult =
    SearchResult(
        patientId = patient.id,
        patientName = patient.name,
        breed = patient.breed,
        microchipId = patient.microchipId,
        recordType = ISearchRepository.TYPE_PATIENT,
        recordId = patient.id,
        date = null,
        snippet = "Date of birth: ${patient.dateOfBirth ?: "not recorded"}.",
    )
