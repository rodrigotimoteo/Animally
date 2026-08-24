package com.github.rodrigotimoteo.animally.llm

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.currentLocale

/** iOS actual: device language decides the assistant locale. */
actual fun assistantStrings(): AssistantStrings {
    val language = NSLocale.currentLocale.objectForKey(NSLocaleLanguageCode) as? String
    return if (language != null && language.lowercase().startsWith("pt")) {
        PtAssistantStrings
    } else {
        EnAssistantStrings
    }
}
