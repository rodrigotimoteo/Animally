package com.github.rodrigotimoteo.animally.llm

/**
 * Shared user-facing cleanup for assistant text.
 *
 * Record and web references use bracketed transport markers while a model is
 * grounded. They are useful to the shared citation mapper, but they are not
 * prose and must never be shown in the chat bubble or restored chat history.
 * Keeping this reducer in common Kotlin also prevents an old persisted answer
 * from bypassing the streaming cleanup on iOS.
 */
internal fun sanitizeAssistantTransportText(text: String): String =
    text
        .replace(assistantScaffoldLineRegex, "")
        .replace(assistantLinkRegex) { link ->
            val label = link.groupValues[1]
            // Preserve citation-shaped Markdown as an internal marker until
            // the source mapper has had a chance to resolve the record.
            if (assistantCitationReferenceRegex.matches(label.trim())) {
                "[${label.trim()}]"
            } else {
                label
            }
        }.replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

/** Removes internal citation syntax and repairs the small orphan tail it can leave behind. */
internal fun sanitizeAssistantDisplayText(text: String): String =
    text
        .let(::sanitizeAssistantTransportText)
        .replace(assistantCitationBlockRegex) { block ->
            if (assistantCitationReferenceRegex.find(block.value) != null) "" else block.value
        }
        // A model may end with prose such as "the visit is documented in"
        // immediately before its internal citation. Do not leave that broken
        // preposition behind after the transport marker is removed.
        .replace(assistantOrphanCitationTailRegex, ".")
        .replace(assistantIncompleteCitationRegex, "")
        .replace(assistantLiteralTagRegex, "")
        .replace(assistantMultiSpaceRegex, " ")
        .replace(assistantSpacedRepeatedPunctuationRegex, "$1")
        .replace(assistantSpaceBeforePunctuationRegex, "$1")
        .replace(assistantLineLeadingSpaceRegex, "")
        .replace(assistantBlankLineRunRegex, "\n\n")
        .trim()

// Kept at file scope so the citation mapper and the history reducer use the
// exact same grammar. The grammar accepts both wire names and humanized names
// such as "FARRIER_VISIT" and "FARRIER VISIT".
internal val assistantCitationBlockRegex = Regex("\\[[^]]*]")
internal val assistantCitationReferenceRegex =
    Regex("([A-Z][A-Z_]*(?:\\s+[A-Z_]+)*)\\s*#(\\d+)", RegexOption.IGNORE_CASE)
internal val assistantIncompleteCitationRegex =
    Regex(
        "\\[(?:[A-Z][A-Z_]*(?:\\s+[A-Z_]+)*)\\s*#\\d*[^]]*$",
        RegexOption.IGNORE_CASE,
    )

private val assistantLinkRegex = Regex("\\[([^\\]]*)]\\(([^)]*)\\)")
private val assistantScaffoldLineRegex = Regex("(?m)^\\s*(?:-{3,}|Question:.*|Context:.*|You are .*)\\s*\\n?")
private val assistantLiteralTagRegex =
    Regex(
        """\[(?:Summary|RECORD_TYPE #ID|PATIENT CENSUS|CARE COUNTS|GESTATIONS|OVERDUE CARE[^]]*)]""",
    )
private val assistantOrphanCitationTailRegex = Regex("(?i)\\s+(?:in|from|via|em|no|na|nos|nas)\\s*[.!?]\\s*$")
private val assistantMultiSpaceRegex = Regex("[ \\t]{2,}")
private val assistantSpaceBeforePunctuationRegex = Regex("[ \\t]+([.,;:!?])")
private val assistantSpacedRepeatedPunctuationRegex = Regex("([.!?])([ \\t]+\\1)+")
private val assistantLineLeadingSpaceRegex = Regex("(?m)^[ \\t]+")
private val assistantBlankLineRunRegex = Regex("\\n{3,}")
