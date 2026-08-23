package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

/**
 * Regression tests proving deleted records leave the global search index.
 *
 * The delete use cases must drop their FTS index row; otherwise soft-deleted
 * consultations keep matching queries forever.
 */
class DeleteSearchIndexTest {
    @Test
    fun givenIndexedConsultationWhenDeletedThenSearchNoLongerFindsIt() {
        val database = createTestDatabase()
        val consultationRepo = ConsultationRepositoryImpl(database)
        val searchRepo = SearchRepositoryImpl(database, database.ownerQueries)
        val save = SaveConsultationUseCase(consultationRepo, searchRepo)
        val delete = DeleteConsultationUseCase(consultationRepo, searchRepo)
        val now = Clock.System.now()

        // The search query joins Patient, so the owning row must exist.
        val patientRepo = PatientRepositoryImpl(database)
        val patientId =
            patientRepo.insertPatient(
                Patient(id = 0, name = "Thunder", species = "Equine", createdAt = now, updatedAt = now),
            )

        val id =
            save(
                Consultation(
                    id = 0,
                    patientId = patientId,
                    date = LocalDate(2026, 8, 1),
                    subjective = "owner reports reduced performance",
                    objective = "murmur auscultated",
                    assessment = "cardio murmur grade 2",
                    plan = "echo follow-up in 4 weeks",
                    vetName = null,
                    nextVisitDate = null,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        assertEquals(1, searchRepo.search("cardio", null, null, null).size)

        delete(id)

        assertEquals(0, searchRepo.search("cardio", null, null, null).size)
    }
}
