package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
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
    private val reminderSchedulerMock: ReminderScheduler = mock(MockMode.autoUnit)
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)
    private lateinit var sut: DeleteCustomReminderUseCase

    @BeforeTest
    fun setup() {
        sut = DeleteCustomReminderUseCase(customReminderRepositoryMock, reminderSchedulerMock, searchRepositoryMock)
    }

    @Test
    fun `when record exists then marks inactive and removes from search index`() {
        every { customReminderRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L
        every { customReminderRepositoryMock.getById(1L) } returns reminder(1L)

        val result = sut(1L)

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(1)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 1L) }
        verify(VerifyMode.exactly(1)) { reminderSchedulerMock.cancel(any()) }
    }

    @Test
    fun `when repository returns zero rows then does not delete from search index`() {
        every { customReminderRepositoryMock.getById(99L) } returns null
        every { customReminderRepositoryMock.setInactive(99L, any<Instant>()) } returns 0L

        val result = sut(99L)

        assertEquals(0L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(99L, any<Instant>()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 99L) }
        verify(VerifyMode.exactly(0)) { reminderSchedulerMock.cancel(any()) }
    }

    @Test
    fun `when repository throws then propagates exception`() {
        every { customReminderRepositoryMock.getById(1L) } returns reminder(1L)
        every { customReminderRepositoryMock.setInactive(1L, any<Instant>()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(RecordType.CustomReminder.wireName, 1L) }
    }

    private fun reminder(id: Long) =
        CustomReminder(
            id = id,
            patientId = 1L,
            title = "Recheck",
            dueDate = kotlinx.datetime.LocalDate(2025, 2, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
}
