package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for retrieving all gestation records of a patient.
 *
 * @param gestationRepository Repository instance for accessing gestation data.
 */
@Single
class GetGestationsByPatientUseCase(
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val calculateGestationUseCase: CalculateGestationUseCase,
) {
    /**
     * Retrieves all active gestation records for the patient with the given [patientId],
     * ordered by breeding date descending.
     *
     * @param patientId The identifier of the patient.
     * @param today Reference date used to project current progress. Defaults to
     *   the device's current local date.
     * @return The list of matching [Gestation] objects with live progress for
     *   ongoing pregnancies.
     */
    operator fun invoke(
        patientId: Long,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): List<Gestation> =
        gestationRepository
            .getByPatient(patientId)
            .map { gestation -> calculateGestationUseCase.withCurrentProgress(gestation, today) }
}
