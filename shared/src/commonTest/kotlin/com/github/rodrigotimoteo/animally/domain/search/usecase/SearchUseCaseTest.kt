package com.github.rodrigotimoteo.animally.domain.search.usecase

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchUseCaseTest {
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private val sut = SearchUseCase(searchRepositoryMock)

    @Test
    fun `when query has multiple words then builds prefix expression`() {
        every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

        sut("lameness chronic", null, null, null)

        verify { searchRepositoryMock.search("lameness* AND chronic*", null, null, null) }
    }

    @Test
    fun `when query has single word then appends prefix token`() {
        every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

        sut("lame", null, null, null)

        verify { searchRepositoryMock.search("lame*", null, null, null) }
    }

    @Test
    fun `when query is blank then returns empty without searching`() {
        val result = sut("   ", null, null, null)

        assertEquals(emptyList(), result)
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.search(any(), any(), any(), any()) }
    }

    @Test
    fun `when filters provided then delegates them`() {
        every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
        val from = LocalDate(2024, 1, 1)
        val to = LocalDate(2024, 12, 31)

        sut("flu", from, to, listOf(ISearchRepository.TYPE_MEDICATION))

        verify { searchRepositoryMock.search("flu*", from, to, listOf(ISearchRepository.TYPE_MEDICATION)) }
    }
}
