package com.github.rodrigotimoteo.animally.llm

/** Per-turn language guidance for cloud and on-device assistant responses. */
object AssistantLanguage {
    private const val PORTUGUESE_INSTRUCTION =
        "LANGUAGE FOR THIS TURN: Answer only in European Portuguese. " +
            "Do not switch languages because the records use another language."
    private const val ENGLISH_INSTRUCTION =
        "LANGUAGE FOR THIS TURN: Answer only in English. " +
            "Do not switch languages because the records use another language."

    fun turnInstruction(query: String): String =
        if (AssistantPrompts.isPortugueseQuery(query)) {
            PORTUGUESE_INSTRUCTION
        } else {
            ENGLISH_INSTRUCTION
        }
}
