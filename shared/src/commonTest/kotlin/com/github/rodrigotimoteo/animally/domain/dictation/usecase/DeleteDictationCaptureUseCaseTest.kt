package com.github.rodrigotimoteo.animally.domain.dictation.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class DeleteDictationCaptureUseCaseTest {
    private val repositoryMock: IDictationCaptureRepository = mock()
    private val filePortMock: DictationFilePort = mock()
    private lateinit var sut: DeleteDictationCaptureUseCase

    @BeforeTest
    fun setup() {
        sut = DeleteDictationCaptureUseCase(repositoryMock, filePortMock)
    }

    private fun capture(path: String? = "/tmp/audio.m4a") =
        DictationCapture(
            id = 1L,
            transcript = "hello",
            audioPath = path,
            durationMillis = 1000L,
            capturedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when capture exists then deletes and removes file`() {
        every { repositoryMock.getById(1L) } returns capture()
        every { repositoryMock.deleteById(1L) } returns 1L
        every { filePortMock.delete("/tmp/audio.m4a") } returns true

        val result = sut(1L)

        assertEquals(true, result)
        verify(VerifyMode.exactly(1)) { repositoryMock.getById(1L) }
        verify(VerifyMode.exactly(1)) { repositoryMock.deleteById(1L) }
        verify(VerifyMode.exactly(1)) { filePortMock.delete("/tmp/audio.m4a") }
    }

    @Test
    fun `when capture not found then returns false`() {
        every { repositoryMock.getById(99L) } returns null

        val result = sut(99L)

        assertEquals(false, result)
        verify(VerifyMode.exactly(1)) { repositoryMock.getById(99L) }
        verify(VerifyMode.exactly(0)) { repositoryMock.deleteById(99L) }
        verify(VerifyMode.exactly(0)) { filePortMock.delete(any<String>()) }
    }

    @Test
    fun `when repository throws then propagates exception`() {
        every { repositoryMock.getById(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { repositoryMock.getById(1L) }
    }
}
