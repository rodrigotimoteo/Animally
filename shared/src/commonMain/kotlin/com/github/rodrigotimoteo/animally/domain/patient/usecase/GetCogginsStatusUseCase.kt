package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Status of a horse's Coggins test relative to [today].
 *
 * @property OVERDUE The test expiry date is before [today].
 * @property EXPIRING_SOON The test expiry date is on or after [today] but within the configured lead window.
 * @property VALID The test expiry date is beyond the configured lead window.
 */
enum class CogginsStatus {
    OVERDUE,
    EXPIRING_SOON,
    VALID,
}

/**
 * A patient whose Coggins test requires attention.
 *
 * @property patient The horse with a Coggins expiry date set.
 * @property status The [CogginsStatus] derived from [expiryDate] relative to today.
 * @property expiryDate The Coggins test expiry date.
 */
data class CogginsAlert(
    val patient: Patient,
    val status: CogginsStatus,
    val expiryDate: LocalDate,
)

/**
 * Returns active patients whose Coggins test is overdue or expiring within [leadDays].
 *
 * Only patients with a [Patient.cogginsExpiryDate] set are considered. Results are sorted
 * with overdue patients first, then by soonest expiry date.
 *
 * @param leadDays The number of days ahead of expiry that still count as expiring soon.
 * @param today The reference date used to derive each alert status.
 * @return The alerts for horses requiring attention, newest risk first.
 */
@Single
class GetCogginsStatusUseCase(
    @Provided private val patientRepository: IPatientRepository,
) {
    operator fun invoke(
        leadDays: Int = 30,
        today: LocalDate,
    ): List<CogginsAlert> {
        val alerts =
            patientRepository
                .getPatientList()
                .mapNotNull { patient ->
                    val expiryDate = patient.cogginsExpiryDate ?: return@mapNotNull null
                    val status =
                        when {
                            expiryDate < today -> CogginsStatus.OVERDUE
                            expiryDate <= today.plus(DatePeriod(days = leadDays)) -> CogginsStatus.EXPIRING_SOON
                            else -> return@mapNotNull null
                        }
                    CogginsAlert(patient = patient, status = status, expiryDate = expiryDate)
                }
        return alerts.sortedWith(
            compareBy<CogginsAlert>(
                { if (it.status == CogginsStatus.OVERDUE) 0 else 1 },
                { it.expiryDate },
            ),
        )
    }
}
