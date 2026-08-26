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
 * Use case for retrieving detailed information about a specific gestation record.
 *
 * @param gestationRepository Repository instance for accessing gestation data.
 */
@Single
class GetGestationDetailUseCase(
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val calculateGestationUseCase: CalculateGestationUseCase,
) {
    /**
     * Retrieves detailed information for a gestation record by its ID.
     *
     * @param id The unique identifier of the gestation record to retrieve.
     * @param today Reference date used to project current progress. Defaults to
     *   the device's current local date.
     * @return The [Gestation] object with live progress when ongoing, or `null`
     *   if no gestation record with the given ID exists.
     */
    operator fun invoke(
        id: Long,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): Gestation? =
        gestationRepository
            .getById(id)
            ?.let { gestation -> calculateGestationUseCase.withCurrentProgress(gestation, today) }
}
