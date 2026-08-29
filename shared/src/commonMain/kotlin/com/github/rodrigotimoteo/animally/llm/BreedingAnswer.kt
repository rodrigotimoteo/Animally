package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/** Emits a human-readable breeding-card answer from Kotlin-owned facts. */
internal suspend fun FlowCollector<RagStreamEvent>.emitBreedingAnswer(
    query: String,
    facts: List<BreedingFact>,
): Boolean {
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    val answer =
        if (facts.size == 1) {
            singleBreedingAnswer(facts.single(), portuguese)
        } else {
            multipleBreedingAnswer(facts, portuguese)
        }
    emit(RagStreamEvent.Chunk(answer))
    emit(RagStreamEvent.Sources(facts.take(MAX_BREEDING_SOURCES).map(::breedingSource)))
    return true
}

private fun singleBreedingAnswer(
    fact: BreedingFact,
    portuguese: Boolean,
): String {
    val date = formatHumanDateShort(fact.event.date)
    return if (portuguese) {
        "${fact.patient.name} foi coberta em $date, há ${fact.elapsedDays} dias. " +
            "Este dado vem do registo de reprodução."
    } else {
        "${fact.patient.name} was bred on $date, ${fact.elapsedDays} days ago. " +
            "This comes from the reproduction record."
    }
}

private fun multipleBreedingAnswer(
    facts: List<BreedingFact>,
    portuguese: Boolean,
): String {
    val heading =
        if (portuguese) {
            "Encontrei ${facts.size} registos de cobrição:"
        } else {
            "I found ${facts.size} recorded breeding entries:"
        }
    val lines =
        facts.take(MAX_BREEDING_SOURCES).joinToString("\n") { fact ->
            val date = formatHumanDateShort(fact.event.date)
            if (portuguese) {
                "- ${fact.patient.name} — coberta em $date, há ${fact.elapsedDays} dias."
            } else {
                "- ${fact.patient.name} — bred on $date, ${fact.elapsedDays} days ago."
            }
        }
    return "$heading\n$lines"
}

private fun breedingSource(fact: BreedingFact): SearchResult =
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
                fact.event.eventType,
                fact.event.details,
                fact.event.breedingType,
                fact.event.stallionName,
            ).joinToString("; "),
    )

private const val MAX_BREEDING_SOURCES = 12
