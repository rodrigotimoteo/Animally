package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveCustomReminderUseCaseTest {
    private val customReminderRepositoryMock: ICustomReminderRepository = mock()

    private val reminderSchedulerMock: ReminderScheduler = mock()

    private lateinit var sut: SaveCustomReminderUseCase

    @BeforeTest
    fun setup() {
        every { reminderSchedulerMock.schedule(any()) } returns Unit
        sut = SaveCustomReminderUseCase(customReminderRepositoryMock, reminderSchedulerMock)
    }

    private fun newReminder(id: Long) =
        CustomReminder(
            id = id,
            patientId = 1L,
            title = "Farrier check",
            dueDate = LocalDate(2025, 2, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then inserts and schedules reminder with generated id`() {
        every { customReminderRepositoryMock.insert(any()) } returns 42L

        val result = sut(newReminder(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) {
            reminderSchedulerMock.schedule(
                matches { it.recordType == "Custom-42" && it.title == "Farrier check" },
            )
        }
    }

    @Test
    fun `when id is non-zero then updates and schedules reminder with existing id`() {
        every { customReminderRepositoryMock.update(any()) } returns 1L

        val result = sut(newReminder(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { customReminderRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) {
            reminderSchedulerMock.schedule(
                matches { it.recordType == "Custom-5" && it.dueDate == LocalDate(2025, 2, 1) },
            )
        }
    }

    @Test
    fun `when id is zero then does not update`() {
        every { customReminderRepositoryMock.insert(any()) } returns 42L

        sut(newReminder(id = 0L))

        verify(VerifyMode.exactly(0)) { customReminderRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) { reminderSchedulerMock.schedule(any()) }
    }
}
