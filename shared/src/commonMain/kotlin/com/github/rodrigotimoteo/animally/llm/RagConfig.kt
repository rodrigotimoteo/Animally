package com.github.rodrigotimoteo.animally.llm

data class RagConfig(
    val maxContextTokens: Int = 4096,
    val systemReserveTokens: Int = 200,
    val queryReserveTokens: Int = 300,
    val responseReserveTokens: Int = 600,
    val chunkTokenCap: Int = 3000,
    /**
     * Hard per-chunk character cap applied when formatting a retrieved
     * snippet, so no single record can dominate the context window (a 3000
     * char snippet alone eats ~750 of the ~3000 usable tokens).
     */
    val chunkCharCap: Int = 1200,
) {
    companion object {
        val DEFAULT = RagConfig()
    }
}

/**
 * Generation policy selected by the engine router before retrieval starts.
 * Foundation Models stay tightly grounded because their small context/model
 * budget benefits from deterministic gates. A configured cloud route can serve
 * both general and grounded turns while still receiving explicit instructions
 * not to invent patient facts.
 */
data class RagQueryPolicy(
    val allowGeneralQuestions: Boolean,
    /** Null keeps the caller's configured budget; cloud supplies its own bound. */
    val maxContextTokens: Int? = null,
) {
    companion object {
        /** Strict policy for Apple Foundation Models and other local engines. */
        val ON_DEVICE =
            RagQueryPolicy(
                allowGeneralQuestions = false,
            )

        /** Bounded cloud policy; provider limits still vary by selected model. */
        val CLOUD =
            RagQueryPolicy(
                allowGeneralQuestions = true,
                maxContextTokens = 16_384,
            )
    }
}
