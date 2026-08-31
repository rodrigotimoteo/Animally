package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Research-readiness issue categories with explicit definitions.
 *
 * Each value maps to a concrete validation rule that can drill down to the
 * affected source rows. No opaque combined quality score is used.
 */
enum class InsightsDataIssueType(
    val displayName: String,
    val definition: String,
    val missingForAnalysisLabel: String,
) {
    UnknownReproductionCategory(
        "Unknown reproduction category",
        "Reproduction event where eventType does not map to Heat, Breeding, " +
            "Pregnancy Check, Foaling or Initial Exam after tolerant parsing — " +
            "counted as Other, missing for analysis.",
        "Unknown category — missing for analysis",
    ),
    MissingVetName(
        "Missing veterinarian name",
        "Activity record where veterinarian attribution (vetName, surgeon, administeredBy, " +
            "farrier, prescribedBy) is null or blank in period — missing for analysis, " +
            "not clinically wrong.",
        "Missing veterinarian name — missing for analysis",
    ),
    UnlinkedOwner(
        "Unlinked owner",
        "Activity record whose patient has no linked owner (Patient.ownerId is null) in period — missing for analysis, not clinically wrong.",
        "Unlinked owner — missing for analysis",
    ),
    FreeTextEmbryoRecipient(
        "Free-text embryo recipient",
        "Embryo transfer with recipientMares containing free-text value within period — missing for analysis as structured linkage not captured.",
        "Free-text recipient — missing for analysis",
    ),
    IncompleteUltrasoundData(
        "Incomplete ultrasound data",
        "Ultrasound record where structured fields (ovary/uterine status, follicle sizes, " +
            "uterine edema/liquid, uterus description) are all null/blank — missing for analysis.",
        "Incomplete structured data — missing for analysis",
    ),
}
