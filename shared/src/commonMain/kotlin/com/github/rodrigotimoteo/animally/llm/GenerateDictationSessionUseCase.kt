package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import com.github.rodrigotimoteo.animally.domain.dictation.dto.SuggestedRecordDto
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json

/**
 * Converts a finalized dictation transcript into the shared structured-session
 * contract. The model is only an extractor: Kotlin owns the schema boundary,
 * and [DictationStore] still validates every decoded record before review/save.
 */
class GenerateDictationSessionUseCase(
    private val llmEngine: RagLlmEngine,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    /** Generates canonical session JSON for the selected [language]. */
    suspend operator fun invoke(
        transcript: String,
        language: String,
    ): String {
        val normalizedTranscript = transcript.trim()
        require(normalizedTranscript.isNotEmpty()) { "There is no transcript to extract." }

        val emissions =
            buildList {
                llmEngine
                    .generate(
                        prompt = buildPrompt(normalizedTranscript, language),
                        instructions = extractionInstructions(language),
                    ).collect { emission ->
                        emission.trim().takeIf(String::isNotEmpty)?.let(::add)
                    }
            }
        if (emissions.isEmpty()) {
            error("The extraction model returned no structured records.")
        }

        // Most engines emit cumulative snapshots, but a few OpenAI-compatible
        // gateways send deltas or append a non-JSON terminal status. Prefer the
        // newest complete snapshot, then try the concatenated stream as a
        // compatibility fallback. No model text is accepted unless it parses
        // into the exact shared session contract.
        val session =
            emissions
                .asReversed()
                .asSequence()
                .mapNotNull { emission -> runCatching { decodeSession(emission) }.getOrNull() }
                .firstOrNull()
                ?: runCatching { decodeSession(emissions.joinToString(separator = "")) }
                    .getOrElse {
                        error("The extraction model returned invalid structured data. Please try the dictation again.")
                    }

        return json.encodeToString(DictatedSessionDto.serializer(), session)
    }

    private fun buildPrompt(
        transcript: String,
        language: String,
    ): String =
        """
        Extract records from this finalized ${languageLabel(language)} veterinary dictation.

        TRANSCRIPT:
        $transcript

        Return one JSON object only. Do not add commentary, Markdown, or a summary.
        Include one record for each ultrasound, weight, or deworming record explicitly spoken.
        Use exactly this envelope and field names:
        {"records":[{"recordType":"ultrasound|weight|deworming","patientName":null,"date":null,"weightKg":null,"ovaryStatus":null,"uterineStatus":null,"follicleSizeMm":null,"drugName":null,"notes":null}]}
        Keep patient names and clinical wording faithful to the transcript. Fill only values explicitly present; use null for every field not spoken. Never infer a diagnosis, treatment, measurement, date, patient, or record. If no supported record was spoken, return {"records":[]}.
        """.trimIndent()

    private fun extractionInstructions(language: String): String {
        val languageRule =
            if (language.equals(PORTUGUESE, ignoreCase = true)) {
                "The transcript is Portuguese (Portugal); preserve Portuguese wording in extracted text fields."
            } else {
                "The transcript is English; preserve English wording in extracted text fields."
            }
        return """
            You are a conservative veterinary dictation extractor. $languageRule
            Your output is consumed by a validator and shown to a person for review.
            Extract only information explicitly stated in the transcript. Do not complete
            missing fields from veterinary knowledge and do not turn a possibility into a fact.
            Output valid JSON only, with the exact DictatedSession envelope requested by the user.
            Supported recordType values are lowercase ultrasound, weight, and deworming only.
            A record needs at least one supported payload field; otherwise omit it.
            """.trimIndent()
    }

    /** Accepts strict JSON, fenced JSON, prose-wrapped JSON, or a bare record array. */
    private fun decodeSession(raw: String): DictatedSessionDto {
        val normalized = stripThinkingBlocks(raw).trim()
        val objectSession =
            jsonObjectCandidates(normalized)
                .asSequence()
                .filter { RECORDS_FIELD_REGEX.containsMatchIn(it) }
                .mapNotNull { candidate ->
                    runCatching { json.decodeFromString<DictatedSessionDto>(candidate) }.getOrNull()
                }.firstOrNull()
        if (objectSession != null) return objectSession

        val arraySession =
            jsonArrayCandidates(normalized)
                .asSequence()
                .mapNotNull { candidate ->
                    runCatching { json.decodeFromString<List<SuggestedRecordDto>>(candidate) }.getOrNull()
                }.firstOrNull()
        if (arraySession != null) return DictatedSessionDto(records = arraySession)

        error("The extraction model returned invalid structured data. Please try the dictation again.")
    }

    private fun stripThinkingBlocks(value: String): String =
        THINKING_BLOCK_REGEXES.fold(value) { current, regex ->
            current.replace(regex, "")
        }

    /** Returns balanced JSON object candidates in source order, outer first. */
    private fun jsonObjectCandidates(value: String): List<String> = balancedJsonCandidates(value, opening = '{')

    /** Returns balanced JSON array candidates in source order, outer first. */
    private fun jsonArrayCandidates(value: String): List<String> = balancedJsonCandidates(value, opening = '[')

    /**
     * Finds complete JSON-shaped substrings without being confused by braces
     * inside quoted notes or by prose surrounding the model response.
     */
    private fun balancedJsonCandidates(
        value: String,
        opening: Char,
    ): List<String> {
        val candidates = mutableListOf<String>()
        value.forEachIndexed { start, character ->
            if (character != opening) return@forEachIndexed
            balancedJsonCandidate(value, start)?.let(candidates::add)
        }
        return candidates
    }

    private fun balancedJsonCandidate(
        value: String,
        start: Int,
    ): String? {
        val expectedClosings = mutableListOf<Char>()
        var inString = false
        var escaped = false

        for (index in start until value.length) {
            val character = value[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }

            when (character) {
                '"' -> inString = true
                '{' -> expectedClosings += '}'
                '[' -> expectedClosings += ']'
                '}', ']' -> {
                    if (
                        expectedClosings.isEmpty() ||
                        expectedClosings.removeAt(expectedClosings.lastIndex) != character
                    ) {
                        return null
                    }
                    if (expectedClosings.isEmpty()) return value.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun languageLabel(language: String): String =
        if (language.equals(PORTUGUESE, ignoreCase = true)) {
            "Portuguese"
        } else {
            "English"
        }

    private companion object {
        const val PORTUGUESE = "portuguese"
        val THINKING_BLOCK_REGEXES =
            listOf(
                Regex("<think(?:ing)?>(?s:.*?)</think(?:ing)>", RegexOption.IGNORE_CASE),
                Regex("<(?:analysis|reasoning)>(?s:.*?)</(?:analysis|reasoning)>", RegexOption.IGNORE_CASE),
                Regex("\\[(?:THINK|THOUGHT)\\](?s:.*?)\\[/(?:THINK|THOUGHT)\\]", RegexOption.IGNORE_CASE),
                Regex(
                    "<\\|(?:thinking|analysis|reasoning|thought)\\|>" +
                        "(?s:.*?)" +
                        "<\\|end_(?:thinking|analysis|reasoning|thought)\\|>",
                    RegexOption.IGNORE_CASE,
                ),
                Regex(
                    "<\\|begin_of_(?:thought|analysis|reasoning)\\|>" +
                        "(?s:.*?)" +
                        "<\\|end_of_(?:thought|analysis|reasoning)\\|>",
                    RegexOption.IGNORE_CASE,
                ),
            )
        val RECORDS_FIELD_REGEX = Regex("\\\"records\\\"\\s*:")
    }
}
