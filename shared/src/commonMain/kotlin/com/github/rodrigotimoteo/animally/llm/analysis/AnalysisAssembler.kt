package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.llm.support.TokenEstimator

internal object AnalysisAssembler {
    fun assemble(blocks: List<String>): String? {
        if (blocks.isEmpty()) return null
        var used = TokenEstimator.estimateTokensWithOverhead(SUMMARY_HEADER)
        return buildString {
            appendLine(SUMMARY_HEADER)
            for ((index, block) in blocks.withIndex()) {
                val cost = TokenEstimator.estimateTokensWithOverhead(block)
                if (index > 0 && used + cost > MAX_SUMMARY_TOKENS) break
                appendLine(block)
                used += cost
            }
        }.trimEnd()
    }
}
