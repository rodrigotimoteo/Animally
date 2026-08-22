package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import dev.mokkery.answering.returns
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

class DeleteOwnerUseCaseTest {
    private val ownerRepositoryMock: IOwnerRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private val searchRepository = FakeSearchRepository()

    private lateinit var sut: DeleteOwnerUseCase

    @BeforeTest
    fun setup() {
        sut = DeleteOwnerUseCase(ownerRepositoryMock, patientRepositoryMock, searchRepository)
    }

    @Test
    fun `when owner has active patients then throws OwnerHasPatientsException`() {
        every { patientRepositoryMock.countPatientsByOwnerId(1L) } returns 2L

        val exception = assertFailsWith<OwnerHasPatientsException> { sut(1L) }

        assertEquals(2L, exception.patientCount)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countPatientsByOwnerId(1L) }
        verify(VerifyMode.exactly(0)) { ownerRepositoryMock.setInactive(1L, any()) }
    }

    @Test
    fun `when owner has no patients then marks owner inactive`() {
        every { patientRepositoryMock.countPatientsByOwnerId(1L) } returns 0L
        every { ownerRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L

        sut(1L)

        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countPatientsByOwnerId(1L) }
        verify(VerifyMode.exactly(1)) { ownerRepositoryMock.setInactive(1L, any<Instant>()) }
    }
}
