package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/** Emits a reproduction-card field without asking the model to infer it. */
internal suspend fun FlowCollector<RagStreamEvent>.emitReproductionAttributeAnswer(
    query: String,
    attribute: ReproductionAttribute,
    facts: List<ReproductionAttributeFact>,
): Boolean {
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    if (facts.isEmpty()) {
        emit(
            RagStreamEvent.Chunk(
                if (portuguese) {
                    "Não encontrei um registo de reprodução para o paciente indicado."
                } else {
                    "I couldn't find a recorded breeding event for the requested patient."
                },
            ),
        )
        return true
    }

    val values = facts.filter { it.value(attribute) != null }
    val answer =
        if (values.isEmpty()) {
            missingValueAnswer(attribute, facts, portuguese)
        } else if (values.size == 1) {
            singleValueAnswer(attribute, values.single(), portuguese)
        } else {
            multipleValueAnswer(attribute, values, portuguese)
        }
    emit(RagStreamEvent.Chunk(answer))
    emit(RagStreamEvent.Sources(facts.take(MAX_REPRODUCTION_ATTRIBUTE_SOURCES).map(::reproductionAttributeSource)))
    return true
}

private fun missingValueAnswer(
    attribute: ReproductionAttribute,
    facts: List<ReproductionAttributeFact>,
    portuguese: Boolean,
): String {
    val patientName = facts.first().patient.name
    return when (attribute) {
        ReproductionAttribute.STALLION ->
            if (portuguese) {
                "O registo de reprodução de $patientName não inclui o nome do garanhão."
            } else {
                "The recorded breeding event for $patientName does not include a stallion name."
            }
        ReproductionAttribute.BREEDING_TYPE ->
            if (portuguese) {
                "O registo de reprodução de $patientName não inclui o método de reprodução."
            } else {
                "The recorded breeding event for $patientName does not include a breeding method."
            }
    }
}

private fun singleValueAnswer(
    attribute: ReproductionAttribute,
    fact: ReproductionAttributeFact,
    portuguese: Boolean,
): String {
    val value = fact.value(attribute) ?: return missingValueAnswer(attribute, listOf(fact), portuguese)
    val date = formatHumanDateShort(fact.event.date)
    return when (attribute) {
        ReproductionAttribute.STALLION ->
            if (portuguese) {
                "${fact.patient.name} foi coberta pelo garanhão $value em $date, segundo o registo de reprodução."
            } else {
                "${fact.patient.name} was bred using stallion $value on $date, according to the reproduction record."
            }
        ReproductionAttribute.BREEDING_TYPE ->
            if (portuguese) {
                "O método de reprodução registado para ${fact.patient.name} foi $value em $date."
            } else {
                "The breeding method recorded for ${fact.patient.name} was $value on $date."
            }
    }
}

private fun multipleValueAnswer(
    attribute: ReproductionAttribute,
    facts: List<ReproductionAttributeFact>,
    portuguese: Boolean,
): String {
    val heading =
        when (attribute) {
            ReproductionAttribute.STALLION ->
                if (portuguese) {
                    "Encontrei estes garanhões registados:"
                } else {
                    "I found these recorded stallions:"
                }
            ReproductionAttribute.BREEDING_TYPE ->
                if (portuguese) {
                    "Encontrei estes métodos de reprodução registados:"
                } else {
                    "I found these recorded breeding methods:"
                }
        }
    val lines =
        facts.take(MAX_REPRODUCTION_ATTRIBUTE_SOURCES).joinToString("\n") { fact ->
            val value = fact.value(attribute) ?: if (portuguese) "não registado" else "not recorded"
            "- ${fact.patient.name} — $value (${formatHumanDateShort(fact.event.date)})"
        }
    return "$heading\n$lines"
}

private fun ReproductionAttributeFact.value(attribute: ReproductionAttribute): String? =
    when (attribute) {
        ReproductionAttribute.STALLION -> event.stallionName
        ReproductionAttribute.BREEDING_TYPE -> event.breedingType
    }?.takeIf(String::isNotBlank)

private fun reproductionAttributeSource(fact: ReproductionAttributeFact): SearchResult =
    SearchResult(
        patientId = fact.patient.id,
        patientName = fact.patient.name,
        breed = fact.patient.breed,
        microchipId = fact.patient.microchipId,
        recordType = RecordType.ReproductionEvent.wireName,
        recordId = fact.event.id,
        date = fact.event.date,
        snippet =
            listOfNotNull(
                "event type: ${fact.event.eventType}",
                fact.event.stallionName?.let { "stallion: $it" },
                fact.event.breedingType?.let { "breeding type: $it" },
                fact.event.details,
            ).joinToString("; "),
    )

private const val MAX_REPRODUCTION_ATTRIBUTE_SOURCES = 12
