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
