package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class GetOwnerDetailUseCaseTest {
    /** Mock of [IOwnerRepository] */
    private val ownerRepositoryMock: IOwnerRepository = mock()

    /** System under test [GetOwnerDetailUseCase] */
    private lateinit var sut: GetOwnerDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetOwnerDetailUseCase(ownerRepositoryMock)
    }

    private fun newOwner() =
        Owner(
            id = 3L,
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when owner exists then sut returns it`() {
        val owner = newOwner()

        every { ownerRepositoryMock.getOwnerById(3L) } returns owner

        val result = sut(3L)

        assertEquals(owner, result)
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.getOwnerById(3L) }
    }

    @Test
    fun `when owner does not exist then sut returns null`() {
        every { ownerRepositoryMock.getOwnerById(3L) } returns null

        val result = sut(3L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.getOwnerById(3L) }
    }
}
