package com.github.rodrigotimoteo.animally.llm.support

import kotlin.math.ceil

/**
 * Consolidated token estimation utility.
 * Centralizes CHARS_PER_TOKEN=4.0 and estimateTokens() duplicates
 * previously in AnalysisContextBuilder and GenerateRagResponseUseCase.
 */
object TokenEstimator {
    const val CHARS_PER_TOKEN = 4.0

    /** ceil(len / 4) — used by GenerateRagResponseUseCase and chunk budgeting. */
    fun estimateTokens(text: String): Int = ceil(text.length / CHARS_PER_TOKEN).toInt()

    /**
     * (len / 4).toInt() + 1 — preserved original AnalysisContextBuilder behavior
     * which counted an extra token overhead per block. Kept distinct to avoid
     * logic change; callers choose the variant matching their original semantics.
     */
    fun estimateTokensWithOverhead(text: String): Int = (text.length / CHARS_PER_TOKEN).toInt() + 1
}
