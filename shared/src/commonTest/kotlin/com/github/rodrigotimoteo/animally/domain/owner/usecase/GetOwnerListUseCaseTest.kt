package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetOwnerListUseCaseTest {
    /** Mock of [IOwnerRepository] */
    private val ownerRepositoryMock: IOwnerRepository = mock()

    /** System under test [GetOwnerListUseCase] */
    private lateinit var sut: GetOwnerListUseCase

    @BeforeTest
    fun setup() {
        sut = GetOwnerListUseCase(ownerRepositoryMock)
    }

    @Test
    fun `when repository returns list then sut returns the same list`() {
        val owners =
            listOf(
                Owner(
                    id = 1L,
                    name = "Alice",
                    email = "alice@example.com",
                    phone = null,
                    address = null,
                    isActive = true,
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
                Owner(
                    id = 2L,
                    name = "Bob",
                    email = null,
                    phone = "12345",
                    address = "Somewhere",
                    isActive = true,
                    createdAt = Instant.fromEpochMilliseconds(1L),
                    updatedAt = Instant.fromEpochMilliseconds(1L),
                ),
            )

        every { ownerRepositoryMock.getOwnerList() } returns owners

        val result = sut()

        assertEquals(expected = owners, actual = result)
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.getOwnerList() }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val owners = emptyList<Owner>()

        every { ownerRepositoryMock.getOwnerList() } returns owners

        val result = sut()

        assertEquals(expected = owners, actual = result)
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.getOwnerList() }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { ownerRepositoryMock.getOwnerList() } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut() }

        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.getOwnerList() }
    }
}
