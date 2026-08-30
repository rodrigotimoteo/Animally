package com.github.rodrigotimoteo.animally.domain.retention

import com.github.rodrigotimoteo.animally.data.assistant.AssistantChatHistoryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.dictation.DictationCaptureRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantRecordSource
import com.github.rodrigotimoteo.animally.domain.assistant.usecase.GetRecentAssistantChatHistoryUseCase
import com.github.rodrigotimoteo.animally.domain.assistant.usecase.SaveAssistantChatTurnUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.GetDictationCapturesUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.SaveDictationCaptureUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.usecase.UpdateDictationCaptureTranscriptUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class AssistantAndDictationRetentionTest {
    @Test
    fun assistantHistoryKeepsNewestFifteenAndRestoresChronologicalOrder() {
        val database = createTestDatabase()
        val repository = AssistantChatHistoryRepositoryImpl(database, database.assistantChatHistoryQueries)
        val save = SaveAssistantChatTurnUseCase(repository)
        val getRecent = GetRecentAssistantChatHistoryUseCase(repository)

        repeat(16) { index ->
            save(
                AssistantChatTurn(
                    question = "Question $index",
                    answer = "Answer $index",
                    source = "CLOUD",
                    interrupted = index == 15,
                    createdAt = Instant.fromEpochMilliseconds(index.toLong()),
                    conversationId = "retention-chat",
                    recordSources =
                        if (index == 1) {
                            listOf(
                                AssistantRecordSource(
                                    patientId = 7L,
                                    patientName = "Lua Nova",
                                    recordType = "FARRIER_VISIT",
                                    recordId = 91L,
                                    date = "2026-08-20",
                                ),
                            )
                        } else {
                            emptyList()
                        },
                ),
            )
        }

        assertEquals((1..15).map { "Question $it" }, getRecent().map { it.question })
        assertEquals("retention-chat", getRecent().first().conversationId)
        assertEquals("Answer 15", getRecent().last().answer)
        assertTrue(getRecent().last().interrupted)
        assertEquals(
            AssistantRecordSource(
                patientId = 7L,
                patientName = "Lua Nova",
                recordType = "FARRIER_VISIT",
                recordId = 91L,
                date = "2026-08-20",
            ),
            getRecent().first().recordSources.single(),
        )
    }

    @Test
    fun dictationArchiveKeepsTranscriptAudioMetadataAndCanBeReadBack() {
        val database = createTestDatabase()
        val repository = DictationCaptureRepositoryImpl(database, database.dictationCaptureQueries)
        val save = SaveDictationCaptureUseCase(repository)
        val update = UpdateDictationCaptureTranscriptUseCase(repository)
        val getAll = GetDictationCapturesUseCase(repository)

        val audioCaptureId =
            save(
                DictationCapture(
                    transcript = "",
                    audioPath = "/private/app/dictations/empty-transcript.caf",
                    durationMillis = 1_250L,
                    capturedAt = Instant.fromEpochMilliseconds(1L),
                ),
            )
        assertTrue(update(audioCaptureId, "Edited transcript after review."))
        save(
            DictationCapture(
                transcript = "Registar o peso da Lua.",
                audioPath = null,
                durationMillis = null,
                capturedAt = Instant.fromEpochMilliseconds(2L),
            ),
        )

        val captures = getAll()
        assertEquals(2, captures.size)
        assertEquals("Registar o peso da Lua.", captures[0].transcript)
        assertEquals("Edited transcript after review.", captures[1].transcript)
        assertEquals("/private/app/dictations/empty-transcript.caf", captures[1].audioPath)
        assertEquals(1_250L, captures[1].durationMillis)
    }
}
