package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/** Emits a human-readable current-pregnancy answer from Kotlin-owned facts. */
internal suspend fun FlowCollector<RagStreamEvent>.emitGestationAnswer(
    query: String,
    facts: List<GestationFact>,
    scopedPatient: String?,
): Boolean {
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    val activeFacts = facts.filter(GestationFact::isActive)
    val sourceFacts = activeFacts.ifEmpty { facts }.take(MAX_GESTATION_SOURCES)
    val answer =
        when {
            activeFacts.isEmpty() -> noActiveGestationAnswer(facts, scopedPatient, portuguese)
            activeFacts.size == 1 -> singleGestationAnswer(activeFacts.single(), portuguese)
            else -> multipleGestationAnswer(activeFacts, portuguese)
        }
    emit(RagStreamEvent.Chunk(answer))
    if (sourceFacts.isNotEmpty()) {
        emit(RagStreamEvent.Sources(sourceFacts.map(::gestationSource)))
    }
    return true
}

private fun singleGestationAnswer(
    fact: GestationFact,
    portuguese: Boolean,
): String {
    val day = fact.progress.gestationDays
    val dueDate = formatHumanDateShort(fact.progress.expectedDueDate)
    return if (portuguese) {
        "${fact.patient.name} tem uma gestação ativa registada: dia $day, " +
            "com parto previsto para $dueDate."
    } else {
        "${fact.patient.name} has an active pregnancy recorded: day $day, " +
            "with expected foaling on $dueDate."
    }
}

private fun multipleGestationAnswer(
    facts: List<GestationFact>,
    portuguese: Boolean,
): String {
    val heading =
        if (portuguese) {
            "Encontrei ${facts.size} gestações ativas nos registos:"
        } else {
            "I found ${facts.size} active pregnancies in the records:"
        }
    val lines =
        facts.joinToString("\n") { fact ->
            val day = fact.progress.gestationDays
            val dueDate = formatHumanDateShort(fact.progress.expectedDueDate)
            if (portuguese) {
                "- ${fact.patient.name} — dia $day, parto previsto para $dueDate."
            } else {
                "- ${fact.patient.name} — day $day, expected foaling $dueDate."
            }
        }
    return "$heading\n$lines"
}

private fun noActiveGestationAnswer(
    facts: List<GestationFact>,
    scopedPatient: String?,
    portuguese: Boolean,
): String {
    val patientName = facts.firstOrNull()?.patient?.name
    return if (portuguese) {
        if (scopedPatient != null || patientName != null) {
            "Não encontrei uma gestação ativa registada para ${patientName ?: scopedPatient}."
        } else {
            "Não encontrei gestações ativas nos registos."
        }
    } else if (scopedPatient != null || patientName != null) {
        "I couldn't find an active pregnancy recorded for ${patientName ?: scopedPatient}."
    } else {
        "I couldn't find any active pregnancies in the records."
    }
}

private fun gestationSource(fact: GestationFact): SearchResult =
    SearchResult(
        patientId = fact.patient.id,
        patientName = fact.patient.name,
        breed = fact.patient.breed,
        microchipId = fact.patient.microchipId,
        recordType = RecordType.Gestation.wireName,
        recordId = fact.gestation.id,
        date = fact.gestation.breedingDate,
        snippet =
            "${fact.gestation.status}; gestation day ${fact.progress.gestationDays}; " +
                "expected foaling ${fact.progress.expectedDueDate}.",
    )

private const val MAX_GESTATION_SOURCES = 12
