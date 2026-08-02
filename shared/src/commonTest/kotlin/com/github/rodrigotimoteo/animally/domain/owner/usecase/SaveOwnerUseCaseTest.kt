package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveOwnerUseCaseTest {
    /** Mock of [IOwnerRepository] */
    private val ownerRepositoryMock: IOwnerRepository = mock()

    /** System under test [SaveOwnerUseCase] */
    private lateinit var sut: SaveOwnerUseCase

    @BeforeTest
    fun setup() {
        sut = SaveOwnerUseCase(ownerRepositoryMock)
    }

    private fun newOwner(id: Long) =
        Owner(
            id = id,
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { ownerRepositoryMock.insertOwner(any()) } returns 42L

        val result = sut(newOwner(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.insertOwner(any()) }
        verify(VerifyMode.exactly(0)) { ownerRepositoryMock.updateOwner(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { ownerRepositoryMock.updateOwner(any()) } returns 1L

        val result = sut(newOwner(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { ownerRepositoryMock.insertOwner(any()) }
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.updateOwner(any()) }
    }
}
