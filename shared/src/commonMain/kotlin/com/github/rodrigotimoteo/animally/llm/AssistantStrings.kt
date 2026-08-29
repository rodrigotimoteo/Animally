package com.github.rodrigotimoteo.animally.llm

/**
 * User-facing assistant strings, localized so the assistant replies in the
 * device language. English is the source of truth; [PtAssistantStrings]
 * carries the PT-PT variants used when the device locale is Portuguese.
 *
 * Selected once at wiring time ([assistantStrings]) - not per message - so
 * mid-session locale changes apply on next launch, matching platform norms.
 */
interface AssistantStrings {
    /** Deterministic reply when retrieval returns nothing (no model call). */
    val noResultsFallback: String

    /** Deterministic reply when the query is too short to search meaningfully. */
    val tooShortReply: String

    /** Friendly reply for greetings and small talk. */
    val greetingReply: String

    /**
     * The exact line the model must emit when the retrieved context lacks the
     * answer; embedded in the system prompt so fabrication has no escape hatch.
     */
    val notFoundInRecords: String

    /** Shown when the model produced no text at all (interrupted/empty stream). */
    val blankReplyFallback: String

    /** Shown when a broader analysis exceeds the safe number of tool rounds. */
    val analysisLimitReply: String

    /**
     * Deterministic refusal for dosage questions asked without medication
     * records in context (no model call). Never advise doses from memory.
     */
    val dosageRefusal: String

    /**
     * Emitted before retrieval starts so the user sees immediate feedback;
     * replaced by the first real chunk (consumers replace their buffer).
     */
    val searchingPlaceholder: String

    /** Safe response when the trusted veterinary reference service is unavailable. */
    val webReferenceUnavailable: String

    /** Safe response when no trusted veterinary reference matches the topic. */
    val webReferenceNoResults: String

    /** Follow-up chip: cited Vaccination record. */
    val followUpNextBooster: String

    /** Follow-up chip: cited Gestation record. */
    val followUpGestationDay: String

    /** Follow-up chip: cited Weight record. */
    val followUpWeightTrend: String

    /** Follow-up chip: cited FarrierVisit record. */
    val followUpNextFarrier: String

    /** Default follow-up chip when nothing was cited: patients. */
    val followUpDefaultPatients: String

    /** Default follow-up chip when nothing was cited: treatments. */
    val followUpDefaultTreatments: String

    /** Default follow-up chip when nothing was cited: dates. */
    val followUpDefaultDates: String
}

/** English (source of truth) assistant strings. */
object EnAssistantStrings : AssistantStrings {
    override val noResultsFallback: String =
        "I couldn't find anything about that in your records. Try asking " +
            "about a horse by name, a treatment, vaccination, or a date."

    override val tooShortReply: String =
        "Could you give me a bit more to go on? Try a horse's name, a " +
            "treatment, or a date."

    override val greetingReply: String =
        "Hello! I’m here to help. Ask me about your patients, treatments, " +
            "vaccinations, gestations, weights, or any other record."

    override val notFoundInRecords: String = "Not found in records."

    override val blankReplyFallback: String =
        "I wasn’t able to finish that answer. Try asking again, or narrow it " +
            "down to a horse, treatment, or date."

    override val analysisLimitReply: String =
        "I couldn’t finish that analysis in one pass. Try narrowing it to a " +
            "horse, date range, or record type."

    override val dosageRefusal: String =
        "I can't advise on dosages. Check the medication record or consult the treating vet."

    override val searchingPlaceholder: String = "Searching your records…"

    override val webReferenceUnavailable: String =
        "I couldn't reach the trusted veterinary references right now, so I won't guess about this medical topic. " +
            "Please try again later or check with the treating vet."

    override val webReferenceNoResults: String =
        "I couldn't find a relevant trusted veterinary reference for that topic, so I won't make up an answer."

    override val followUpNextBooster: String = "When is the next booster due?"

    override val followUpGestationDay: String = "What day of gestation is she?"

    override val followUpWeightTrend: String = "How has her weight changed?"

    override val followUpNextFarrier: String = "When is the next farrier visit?"

    override val followUpDefaultPatients: String = "Which patients do I have?"

    override val followUpDefaultTreatments: String = "Any recent treatments?"

    override val followUpDefaultDates: String = "What happened this month?"
}

/** PT-PT assistant strings, phrased naturally for veterinary use. */
object PtAssistantStrings : AssistantStrings {
    override val noResultsFallback: String =
        "Não encontrei nada sobre isso nos seus registos. Experimente " +
            "perguntar pelo nome de um cavalo, um tratamento, uma vacinação " +
            "ou uma data."

    override val tooShortReply: String =
        "Pode dar-me mais alguns detalhes? Experimente o nome de um cavalo, " +
            "um tratamento ou uma data."

    override val greetingReply: String =
        "Olá! Estou aqui para ajudar. Pergunte-me sobre os seus pacientes, " +
            "tratamentos, vacinas, gestações, pesos ou qualquer registo."

    override val notFoundInRecords: String = "Não encontrado nos registos."

    override val blankReplyFallback: String =
        "Não consegui terminar essa resposta. Tente novamente ou indique um " +
            "cavalo, tratamento ou data."

    override val analysisLimitReply: String =
        "Não consegui terminar essa análise de uma só vez. Tente indicar um " +
            "cavalo, intervalo de datas ou tipo de registo."

    override val dosageRefusal: String =
        "Não posso aconselhar sobre doses. Consulte o registo do medicamento ou o veterinário responsável."

    override val searchingPlaceholder: String = "A pesquisar nos seus registos…"

    override val webReferenceUnavailable: String =
        "Não consegui contactar as referências veterinárias de confiança neste momento, por isso não vou " +
            "adivinhar sobre este tema médico. " +
            "Tente novamente mais tarde ou confirme com o veterinário responsável."

    override val webReferenceNoResults: String =
        "Não encontrei uma referência veterinária de confiança relevante para esse tema, por isso não vou " +
            "inventar uma resposta."

    override val followUpNextBooster: String = "Quando é a próxima vacina?"

    override val followUpGestationDay: String = "Que dia de gestação tem ela?"

    override val followUpWeightTrend: String = "Como evoluiu o peso dela?"

    override val followUpNextFarrier: String = "Quando é a próxima visita do ferrador?"

    override val followUpDefaultPatients: String = "Quais pacientes tenho?"

    override val followUpDefaultTreatments: String = "Há tratamentos recentes?"

    override val followUpDefaultDates: String = "O que aconteceu este mês?"
}

/**
 * Resolves the assistant strings from the device locale. Wired once where the
 * RAG pipeline is constructed (see LlmModule); platform actuals read the OS
 * preferred-language list.
 */
expect fun assistantStrings(): AssistantStrings
