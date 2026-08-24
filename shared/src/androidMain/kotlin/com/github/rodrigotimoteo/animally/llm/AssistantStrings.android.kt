package com.github.rodrigotimoteo.animally.llm

import java.util.Locale

/** Android actual: default system locale decides the assistant locale. */
actual fun assistantStrings(): AssistantStrings =
    if (Locale.getDefault().language.startsWith("pt")) {
        PtAssistantStrings
    } else {
        EnAssistantStrings
    }
