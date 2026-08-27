package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import com.github.rodrigotimoteo.animally.domain.dictation.dto.SuggestedRecordDto
import kotlinx.coroutines.flow.lastOrNull
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

        val raw =
            llmEngine
                .generate(
                    prompt = buildPrompt(normalizedTranscript, language),
                    instructions = extractionInstructions(language),
                ).lastOrNull()
                ?.trim()
                .orEmpty()
        if (raw.isEmpty()) {
            error("The extraction model returned no structured records.")
        }

        return json.encodeToString(DictatedSessionDto.serializer(), decodeSession(raw))
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
        val normalized = raw.replace(THINKING_BLOCK_REGEX, "").trim()
        val objectCandidate = jsonObjectCandidate(normalized)
        if (objectCandidate != null && RECORDS_FIELD_REGEX.containsMatchIn(objectCandidate)) {
            runCatching { json.decodeFromString<DictatedSessionDto>(objectCandidate) }
                .getOrNull()
                ?.let { return it }
        }

        val arrayCandidate = jsonArrayCandidate(normalized)
        if (arrayCandidate != null) {
            runCatching { json.decodeFromString<List<SuggestedRecordDto>>(arrayCandidate) }
                .getOrNull()
                ?.let { return DictatedSessionDto(records = it) }
        }

        error("The extraction model returned invalid structured data. Please try the dictation again.")
    }

    private fun jsonObjectCandidate(value: String): String? {
        val start = value.indexOf('{')
        val end = value.lastIndexOf('}')
        return if (start >= 0 && end > start) value.substring(start, end + 1) else null
    }

    private fun jsonArrayCandidate(value: String): String? {
        val start = value.indexOf('[')
        val end = value.lastIndexOf(']')
        return if (start >= 0 && end > start) value.substring(start, end + 1) else null
    }

    private fun languageLabel(language: String): String =
        if (language.equals(PORTUGUESE, ignoreCase = true)) {
            "Portuguese"
        } else {
            "English"
        }

    private companion object {
        const val PORTUGUESE = "portuguese"
        val THINKING_BLOCK_REGEX = Regex("<think(?:ing)?>(?s:.*?)</think(?:ing)>", RegexOption.IGNORE_CASE)
        val RECORDS_FIELD_REGEX = Regex("\\\"records\\\"\\s*:")
    }
}
