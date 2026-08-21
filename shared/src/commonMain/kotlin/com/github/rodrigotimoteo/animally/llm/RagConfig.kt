package com.github.rodrigotimoteo.animally.llm

data class RagConfig(
    val maxContextTokens: Int = 4096,
    val systemReserveTokens: Int = 200,
    val queryReserveTokens: Int = 300,
    val responseReserveTokens: Int = 600,
    val chunkTokenCap: Int = 3000,
) {
    companion object {
        val DEFAULT = RagConfig()
    }
}
