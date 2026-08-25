package com.github.rodrigotimoteo.animally.llm.cloud

/**
 * Known OpenAI-compatible providers selectable in the Cloud AI settings. A preset
 * fills the endpoint base URL (still editable afterwards); [CUSTOM] keeps the
 * manual-entry behavior. Local runtimes ([OLLAMA], [LM_STUDIO]) need no API key.
 */
enum class CloudLlmProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val requiresApiKey: Boolean,
    /** True for local OpenAI-compatible runtimes where the app owns the budget. */
    val isLocalRuntime: Boolean = false,
    /** Local runtimes point at localhost, only reachable on desktop builds. */
    val visibleOnMobile: Boolean = true,
) {
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1", true),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", true),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1", true),
    TOGETHER("together", "Together", "https://api.together.xyz/v1", true),
    ZEN("zen", "OpenCode Zen", "https://opencode.ai/zen/v1", true),
    OPENCODE_GO("opencode_go", "OpenCode Go", "https://opencode.ai/zen/go/v1", true),
    OLLAMA(
        "ollama",
        "Ollama (local)",
        "http://localhost:11434/v1",
        false,
        isLocalRuntime = true,
        visibleOnMobile = false,
    ),
    LM_STUDIO(
        "lm_studio",
        "LM Studio (local)",
        "http://localhost:1234/v1",
        false,
        isLocalRuntime = true,
        visibleOnMobile = false,
    ),
    CUSTOM("custom", "Custom", "", true),
    ;

    companion object {
        /** Resolves [id] to a preset; unknown/blank ids fall back to [CUSTOM]. */
        fun fromId(id: String?): CloudLlmProviderPreset = entries.firstOrNull { it.id == id } ?: CUSTOM

        /**
         * Maps a stored base URL back to its preset, ignoring trailing slashes so
         * hand-edited URLs still resolve. Returns null for unmatched/custom URLs.
         */
        fun fromBaseUrl(url: String?): CloudLlmProviderPreset? {
            val normalized = url?.trim()?.trimEnd('/')
            if (normalized.isNullOrEmpty()) return null
            return entries.firstOrNull { it.baseUrl.isNotEmpty() && it.baseUrl == normalized }
        }
    }
}
