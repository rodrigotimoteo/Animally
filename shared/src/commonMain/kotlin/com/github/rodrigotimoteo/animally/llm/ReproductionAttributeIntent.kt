package com.github.rodrigotimoteo.animally.llm

/** Reproduction-card fields that are safe to project directly from storage. */
internal enum class ReproductionAttribute {
    STALLION,
    BREEDING_TYPE,
}

/** Detects questions whose answer is a named reproduction-card field. */
internal object ReproductionAttributeIntent {
    private val stallionRegex =
        Regex(
            "\\b(stallion|sire|stud|garanh[aã]o|reprodutor)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val breedingTypeRegex =
        Regex(
            "\\b(breeding\\s+(?:method|type)|what\\s+type\\s+of\\s+breeding|" +
                "tipo\\s+de\\s+(?:reprodução|reproducao|cobertura)|" +
                "m[eé]todo\\s+de\\s+(?:reprodução|reproducao|cobertura)|" +
                "como\\s+foi\\s+(?:coberta|inseminada))\\b",
            RegexOption.IGNORE_CASE,
        )

    fun requestedAttribute(query: String): ReproductionAttribute? =
        when {
            stallionRegex.containsMatchIn(query) -> ReproductionAttribute.STALLION
            breedingTypeRegex.containsMatchIn(query) -> ReproductionAttribute.BREEDING_TYPE
            else -> null
        }
}
