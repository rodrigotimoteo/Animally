package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/** Emits the reproductive-card timeline without asking a model to infer an outcome. */
internal suspend fun FlowCollector<RagStreamEvent>.emitBreedingOutcomeAnswer(
    query: String,
    facts: List<BreedingOutcomeFact>,
): Boolean {
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    if (facts.isEmpty()) {
        emit(
            RagStreamEvent.Chunk(
                if (portuguese) {
                    "Não existem registos de resultado reprodutivo para o paciente indicado."
                } else {
                    "No breeding outcome events are recorded for the requested patient."
                },
            ),
        )
        return true
    }
    val heading =
        if (portuguese) {
            "Registos reprodutivos encontrados:"
        } else {
            "Recorded reproductive events:"
        }
    val lines =
        facts.take(MAX_BREEDING_OUTCOME_SOURCES).joinToString("\n") { fact ->
            val event = fact.event
            val details = event.details?.takeIf(String::isNotBlank) ?: "No additional details recorded"
            val connector = if (portuguese) "em" else "on"
            val punctuation = if (details.lastOrNull() in setOf('.', '!', '?')) "" else "."
            "- ${fact.patient.name} — ${event.eventType} $connector ${formatHumanDateShort(event.date)}: " +
                "$details$punctuation"
        }
    emit(RagStreamEvent.Chunk("$heading\n$lines"))
    emit(RagStreamEvent.Sources(facts.take(MAX_BREEDING_OUTCOME_SOURCES).map(::breedingOutcomeSource)))
    return true
}

private fun breedingOutcomeSource(fact: BreedingOutcomeFact): SearchResult =
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
                fact.event.stallionName,
                fact.event.breedingType,
            ).joinToString("; "),
    )

private const val MAX_BREEDING_OUTCOME_SOURCES = 12
