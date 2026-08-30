package com.github.rodrigotimoteo.animally.llm.prompts

/**
 * Domain synonym groups for retrieval recall extracted from AssistantPrompts.
 * When a query token matches a member, the group's remaining members are
 * appended as OR-terms so natural phrasing ("in foal", "shod") can still
 * reach records indexed under different vocabulary.
 */
internal object SynonymExpander {
    private const val PORTUGUESE_VACCINATION = "vacinação"
    private const val PORTUGUESE_DEWORMING = "desparasitação"

    /**
     * Domain synonym groups for retrieval recall. Multi-word members are
     * allowed; they are emitted as quoted FTS phrases. At most
     * [MAX_SYNONYM_GROUPS] groups are expanded per query.
     */
    val SYNONYM_GROUPS: List<List<String>> =
        listOf(
            listOf(
                "pregnant",
                "in foal",
                "gestation",
                "foaling",
                "bred",
                "prenha",
                "prenhez",
                "gravidez",
                "gestação",
                "gestacao",
            ),
            listOf(
                "stallion",
                "sire",
                "garanhão",
                "garanhao",
                "reprodutor",
                "breeding",
                "insemination",
                "mating",
            ),
            listOf("shod", "shoeing", "shoes", "trim", "farrier", "ferrador", "ferragem", "casco", "cascos"),
            listOf(
                "vaccination",
                "vaccine",
                "booster",
                "shot",
                PORTUGUESE_VACCINATION,
                "vacina",
                "reforço",
                "reforco",
            ),
            listOf(
                "deworming",
                "dewormer",
                "wormer",
                PORTUGUESE_DEWORMING,
                "desparasitacao",
                "vermifugação",
                "vermifugacao",
            ),
            listOf("ultrasound", "ecografia", "ultrassom"),
            listOf("embryo transfer", "flush", "donor", "recipient"),
            listOf("colic", "abdominal pain"),
            listOf("tendon", "tendinitis"),
        )

    /** Maximum synonym groups expanded into a single query. */
    const val MAX_SYNONYM_GROUPS = 2

    /**
     * OR-terms contributed by synonym expansion for [tokens].
     */
    fun expansionTerms(tokens: List<String>): List<String> {
        val lowered = tokens.map(String::lowercase).toSet()
        val phrase = tokens.joinToString(" ").lowercase()
        val expansions = mutableListOf<String>()
        SYNONYM_GROUPS
            .filter { group -> group.matchesAny(lowered, phrase) }
            .take(MAX_SYNONYM_GROUPS)
            .forEach { group -> expansions += group.expansionTermsForGroup(lowered) }
        return expansions
    }

    private fun List<String>.matchesAny(
        loweredTokens: Set<String>,
        phrase: String,
    ): Boolean =
        any { term ->
            if (' ' in term) {
                phrase.contains(term)
            } else {
                loweredTokens.any { token -> token == term || singularize(token) == term }
            }
        }

    private fun List<String>.expansionTermsForGroup(loweredTokens: Set<String>): List<String> =
        mapNotNull { term ->
            when {
                ' ' in term -> "\"$term\"*"
                term in loweredTokens -> null
                else -> "$term*"
            }
        }

    private const val PLURAL_MIN_TOKEN_LENGTH = 4
    private const val PLURAL_IES_SUFFIX_LENGTH = 3

    private fun singularize(token: String): String =
        when {
            token.length > PLURAL_MIN_TOKEN_LENGTH && token.endsWith("ies") ->
                token.dropLast(PLURAL_IES_SUFFIX_LENGTH) + "y"
            token.length > PLURAL_MIN_TOKEN_LENGTH && token.endsWith("s") ->
                token.dropLast(1)
            else -> token
        }
}
