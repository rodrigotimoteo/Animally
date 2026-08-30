package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
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
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class DeleteDewormingUseCaseTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)
    private lateinit var sut: DeleteDewormingUseCase

    @BeforeTest
    fun setup() {
        sut = DeleteDewormingUseCase(dewormingRepositoryMock, searchRepositoryMock)
    }

    @Test
    fun `when record exists then marks inactive and removes from search index`() {
        every { dewormingRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L

        sut(1L)

        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(1)) { searchRepositoryMock.deleteRecord(RecordType.Deworming.wireName, 1L) }
    }

    @Test
    fun `when repository returns zero rows then still deletes from search index`() {
        every { dewormingRepositoryMock.setInactive(99L, any<Instant>()) } returns 0L

        sut(99L)

        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.setInactive(99L, any<Instant>()) }
        verify(VerifyMode.exactly(1)) { searchRepositoryMock.deleteRecord(RecordType.Deworming.wireName, 99L) }
    }

    @Test
    fun `when repository throws then propagates exception`() {
        every { dewormingRepositoryMock.setInactive(1L, any<Instant>()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(RecordType.Deworming.wireName, 1L) }
    }
}
