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

    /**
     * Deterministic refusal for dosage questions asked without medication
     * records in context (no model call). Never advise doses from memory.
     */
    val dosageRefusal: String

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
        "Hello! Ask me about your patients - treatments, vaccinations, " +
            "gestations, weights, or any record."

    override val notFoundInRecords: String = "Not found in records."

    override val blankReplyFallback: String =
        "I could not find anything relevant in the records."

    override val dosageRefusal: String =
        "I can't advise on dosages. Check the medication record or consult the treating vet."

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
        "Olá! Pergunte-me sobre os seus pacientes - tratamentos, vacinas, " +
            "gestações, pesos ou qualquer registo."

    override val notFoundInRecords: String = "Não encontrado nos registos."

    override val blankReplyFallback: String =
        "Não consegui encontrar nada relevante nos registos."

    override val dosageRefusal: String =
        "Não posso aconselhar sobre doses. Consulte o registo do medicamento ou o veterinário responsável."

    override val followUpNextBooster: String = "Quando é a próxima vacina?"

    override val followUpGestationDay: String = "Que dia de gestação tem ela?"

    override val followUpWeightTrend: String = "Como evoluiu o peso dela?"

    override val followUpNextFarrier: String = "Quando é a próxima ferradura?"

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
