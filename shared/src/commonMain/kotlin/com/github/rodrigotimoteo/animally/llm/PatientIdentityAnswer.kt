package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.datetime.LocalDate

/**
 * Conservative intent detection for facts stored directly on a patient row.
 * Keeping this separate from record-type detection prevents a cloud model from
 * rewriting exact identity values such as a date of birth.
 */
internal object PatientIdentityAnswer {
    enum class Field {
        DATE_OF_BIRTH,
        AGE,
        SPECIES,
        BREED,
        GENDER,
        COLOR,
        MICROCHIP,
        UELN,
        REGISTRATION,
        STABLE_LOCATION,
    }

    private val dateOfBirthRegex =
        Regex(
            "\\b(date\\s+of\\s+birth|birth\\s+date|dob|born|birthday|" +
                "data\\s+de\\s+nascimento|nasceu|aniversário|aniversario)\\b",
            RegexOption.IGNORE_CASE,
        )
    private const val IDENTITY_FIELD_PATTERN =
        "(?:breed|age|species|sex|gender|colour|color|microchip(?:\\s+number)?|date\\s+of\\s+birth|" +
            "data\\s+de\\s+nascimento|ueln|life\\s+number|registration|registered|studbook|stable|stabled|" +
            "stabling|location|raça|raca|idade|espécie|especie|sexo|género|genero|cor|pelagem|microchip|" +
            "registo|inscrição|inscricao|localização|localizacao)"
    private val identityQuestionRegex =
        Regex(
            "\\bhow\\s+old\\s+is\\b|" +
                "\\b(?:what|which)\\s+$IDENTITY_FIELD_PATTERN" +
                "(?:\\s*(?:,\\s*(?:and\\s+)?|and\\s+|or\\s+|&\\s+)$IDENTITY_FIELD_PATTERN)*" +
                "\\s+(?:is|are|does|do|has|have)\\b|" +
                "\\b(?:what|which)\\s+(?:is|are)\\s+(?:the\\s+)?$IDENTITY_FIELD_PATTERN" +
                "(?:\\s+(?:of|for))?\\b|" +
                "\\b(?:what|which)\\s+(?:is|are)\\s+[^?\\n]{1,50}['’]s\\s+" +
                "$IDENTITY_FIELD_PATTERN\\b|" +
                "\\b$IDENTITY_FIELD_PATTERN\\s+(?:of|for)\\b|" +
                "\\b(?:qual|que)\\s+(?:é|e)\\s+(?:a\\s+)?$IDENTITY_FIELD_PATTERN\\b|" +
                "\\b(?:que|qual)\\s+idade\\s+tem\\b|" +
                "\\bwhere\\s+[^?\\n]{1,50}\\b(?:stable|stabled|stabling|location)\\b|" +
                "\\bonde\\s+[^?\\n]{1,50}\\b(?:alojad[oa]|localiza)\\b|" +
                "\\bwhen\\s+was\\s+[^?\\n]+\\b(?:born|birthday)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val ageRegex = Regex("\\b(?:age|how\\s+old|idade|quantos\\s+anos)\\b", RegexOption.IGNORE_CASE)
    private val speciesRegex = Regex("\\b(?:species|espécie|especie)\\b", RegexOption.IGNORE_CASE)
    private val breedRegex = Regex("\\b(?:breed|raça|raca)\\b", RegexOption.IGNORE_CASE)
    private val genderRegex = Regex("\\b(?:sex|gender|sexo|género|genero)\\b", RegexOption.IGNORE_CASE)
    private val colorRegex = Regex("\\b(?:colour|color|cor|pelagem)\\b", RegexOption.IGNORE_CASE)
    private val microchipRegex = Regex("\\b(?:microchip|chip)\\b", RegexOption.IGNORE_CASE)
    private val uelnRegex = Regex("\\b(?:ueln|life\\s+number)\\b", RegexOption.IGNORE_CASE)
    private val registrationRegex =
        Regex(
            "\\b(?:registration|registered|studbook|registo|inscrição|inscricao)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val stableLocationRegex =
        Regex(
            "\\b(?:stable|stabled|stabling|location|where\\s+is|localização|localizacao|" +
                "está\\s+alojad[oa]|esta\\s+alojad[oa])\\b",
            RegexOption.IGNORE_CASE,
        )

    fun requestedFields(query: String): List<Field> =
        if (!identityQuestionRegex.containsMatchIn(query)) {
            emptyList()
        } else {
            listOfNotNull(
                Field.DATE_OF_BIRTH.takeIf { dateOfBirthRegex.containsMatchIn(query) },
                Field.AGE.takeIf { ageRegex.containsMatchIn(query) },
                Field.SPECIES.takeIf { speciesRegex.containsMatchIn(query) },
                Field.BREED.takeIf { breedRegex.containsMatchIn(query) },
                Field.GENDER.takeIf { genderRegex.containsMatchIn(query) },
                Field.COLOR.takeIf { colorRegex.containsMatchIn(query) },
                Field.MICROCHIP.takeIf { microchipRegex.containsMatchIn(query) },
                Field.UELN.takeIf { uelnRegex.containsMatchIn(query) },
                Field.REGISTRATION.takeIf { registrationRegex.containsMatchIn(query) },
                Field.STABLE_LOCATION.takeIf { stableLocationRegex.containsMatchIn(query) },
            )
        }

    fun isIdentityQuestion(query: String): Boolean = requestedFields(query).isNotEmpty()
}

/**
 * Emits exact patient-row projections for one resolved active patient. Missing
 * values are answered deterministically so a cloud model cannot turn an absent
 * field into a plausible identity fact.
 */
internal suspend fun FlowCollector<RagStreamEvent>.emitPatientIdentityAnswer(
    query: String,
    scopedPatient: String?,
    patientNameMentioned: Boolean,
    patientRepository: IPatientRepository?,
    today: LocalDate,
): Boolean {
    val fields = PatientIdentityAnswer.requestedFields(query)
    if (fields.isEmpty()) return false
    if (!patientNameMentioned) return false
    val patientName = scopedPatient
    val repository = patientRepository
    return if (patientName == null || repository == null) {
        false
    } else {
        val patient = repository.getPatientList().singleOrNull { it.name.equals(patientName, ignoreCase = true) }
        if (patient == null) {
            false
        } else {
            val portuguese = AssistantPrompts.isPortugueseQuery(query)
            val answer =
                fields.joinToString("\n") { field ->
                    patientIdentitySentence(patient, field, portuguese, today)
                }
            emit(RagStreamEvent.Chunk(answer))
            emit(RagStreamEvent.Sources(listOf(patientSource(patient))))
            true
        }
    }
}

private fun patientIdentitySentence(
    patient: Patient,
    field: PatientIdentityAnswer.Field,
    portuguese: Boolean,
    today: LocalDate,
): String {
    val value = patientIdentityValue(patient, field, today)
    val fieldName = identityFieldName(field, portuguese)
    if (value == null) {
        return missingIdentitySentence(patient.name, field, fieldName, portuguese)
    }
    return if (portuguese) {
        "${patient.name}: $fieldName é $value."
    } else {
        "${patient.name}'s $fieldName is $value."
    }
}

private fun patientIdentityValue(
    patient: Patient,
    field: PatientIdentityAnswer.Field,
    today: LocalDate,
): String? =
    when (field) {
        PatientIdentityAnswer.Field.DATE_OF_BIRTH -> patient.dateOfBirth?.let(::formatHumanDateShort)
        PatientIdentityAnswer.Field.AGE -> patient.dateOfBirth?.let { ageOn(it, today)?.toString() }
        PatientIdentityAnswer.Field.SPECIES -> patient.species.takeIf { it.isNotBlank() }
        PatientIdentityAnswer.Field.BREED -> patient.breed
        PatientIdentityAnswer.Field.GENDER -> patient.gender
        // The patient schema deliberately has no colour field. Keep this
        // deterministic instead of asking a model to infer it from notes.
        PatientIdentityAnswer.Field.COLOR -> null
        PatientIdentityAnswer.Field.MICROCHIP -> patient.microchipId
        PatientIdentityAnswer.Field.UELN -> patient.ueln
        PatientIdentityAnswer.Field.REGISTRATION -> patient.registrationNumber
        PatientIdentityAnswer.Field.STABLE_LOCATION -> patient.stableLocation
    }

private fun missingIdentitySentence(
    patientName: String,
    field: PatientIdentityAnswer.Field,
    fieldName: String,
    portuguese: Boolean,
): String =
    if (field == PatientIdentityAnswer.Field.DATE_OF_BIRTH) {
        if (portuguese) {
            "A data de nascimento de $patientName não está registada."
        } else {
            "The date of birth for $patientName is not recorded."
        }
    } else if (portuguese) {
        "$patientName: $fieldName não está registado."
    } else {
        "$patientName's $fieldName is not recorded."
    }

private val portugueseIdentityFieldNames =
    mapOf(
        PatientIdentityAnswer.Field.DATE_OF_BIRTH to "data de nascimento",
        PatientIdentityAnswer.Field.AGE to "idade",
        PatientIdentityAnswer.Field.SPECIES to "espécie",
        PatientIdentityAnswer.Field.BREED to "raça",
        PatientIdentityAnswer.Field.GENDER to "sexo",
        PatientIdentityAnswer.Field.COLOR to "cor",
        PatientIdentityAnswer.Field.MICROCHIP to "microchip",
        PatientIdentityAnswer.Field.UELN to "UELN",
        PatientIdentityAnswer.Field.REGISTRATION to "número de registo",
        PatientIdentityAnswer.Field.STABLE_LOCATION to "localização",
    )

private val englishIdentityFieldNames =
    mapOf(
        PatientIdentityAnswer.Field.DATE_OF_BIRTH to "date of birth",
        PatientIdentityAnswer.Field.AGE to "age",
        PatientIdentityAnswer.Field.SPECIES to "species",
        PatientIdentityAnswer.Field.BREED to "breed",
        PatientIdentityAnswer.Field.GENDER to "sex",
        PatientIdentityAnswer.Field.COLOR to "colour",
        PatientIdentityAnswer.Field.MICROCHIP to "microchip",
        PatientIdentityAnswer.Field.UELN to "UELN",
        PatientIdentityAnswer.Field.REGISTRATION to "registration number",
        PatientIdentityAnswer.Field.STABLE_LOCATION to "stable location",
    )

private fun identityFieldName(
    field: PatientIdentityAnswer.Field,
    portuguese: Boolean,
): String = (if (portuguese) portugueseIdentityFieldNames else englishIdentityFieldNames).getValue(field)

private fun ageOn(
    dateOfBirth: LocalDate,
    today: LocalDate,
): Int? {
    if (dateOfBirth > today) return null
    val birthdayPassed =
        today.month.ordinal > dateOfBirth.month.ordinal ||
            today.month.ordinal == dateOfBirth.month.ordinal &&
            today.day >= dateOfBirth.day
    return today.year - dateOfBirth.year - if (birthdayPassed) 0 else 1
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
        snippet =
            "Species: ${patient.species}; breed: ${patient.breed ?: "not recorded"}; " +
                "date of birth: ${patient.dateOfBirth ?: "not recorded"}; " +
                "sex: ${patient.gender ?: "not recorded"}; " +
                "microchip: ${patient.microchipId ?: "not recorded"}.",
    )
