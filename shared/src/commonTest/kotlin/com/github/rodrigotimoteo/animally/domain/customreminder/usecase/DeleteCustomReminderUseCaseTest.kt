package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import dev.mokkery.MockMode
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

class DeleteCustomReminderUseCaseTest {
    private val customReminderRepositoryMock: ICustomReminderRepository = mock()
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)
    private lateinit var sut: DeleteCustomReminderUseCase

    @BeforeTest
    fun setup() {
        sut = DeleteCustomReminderUseCase(customReminderRepositoryMock, searchRepositoryMock)
    }

    @Test
    fun `when record exists then marks inactive and removes from search index`() {
        every { customReminderRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L

        val result = sut(1L)

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(1)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 1L) }
    }

    @Test
    fun `when repository returns zero rows then does not delete from search index`() {
        every { customReminderRepositoryMock.setInactive(99L, any<Instant>()) } returns 0L

        val result = sut(99L)

        assertEquals(0L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(99L, any<Instant>()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 99L) }
    }

    @Test
    fun `when repository throws then propagates exception`() {
        every { customReminderRepositoryMock.setInactive(1L, any<Instant>()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 1L) }
    }
}
