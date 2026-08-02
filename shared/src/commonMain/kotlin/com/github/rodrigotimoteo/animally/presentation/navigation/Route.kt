package com.github.rodrigotimoteo.animally.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation routes for the Animally app
 *
 * @author rodrigotimoteo
 */
@Serializable
sealed interface Route : NavKey {
    /**
     * The default route when the app is launched.
     */
    @Serializable
    data object PatientList : Route

    /**
     * The route for the global search screen.
     */
    @Serializable
    data object Search : Route

    /**
     * The route for displaying the list of owners.
     */
    @Serializable
    data object OwnerList : Route

    /**
     * The route for displaying the settings.
     */
    @Serializable
    data object Settings : Route

    /**
     * The route for displaying patient details.
     */
    @Serializable
    data class PatientDetail(
        val patientId: Long,
    ) : Route

    /**
     * The route for adding or editing a patient.
     */
    @Serializable
    data class AddEditPatient(
        val patientId: Long? = null,
    ) : Route

    /**
     * The route for displaying the detail of an owner.
     */
    @Serializable
    data class OwnerDetail(
        val ownerId: Long,
    ) : Route

    /**
     * The route for adding or editing an owner.
     */
    @Serializable
    data class AddEditOwner(
        val ownerId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing an anamnese.
     */
    @Serializable
    data class AddEditAnamnese(
        val patientId: Long,
        val anamneseId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a consultation.
     */
    @Serializable
    data class AddEditConsultation(
        val patientId: Long,
        val consultationId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a vaccination.
     */
    @Serializable
    data class AddEditVaccination(
        val patientId: Long,
        val vaccinationId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a weight entry.
     */
    @Serializable
    data class AddEditWeight(
        val patientId: Long,
        val weightId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a deworming treatment.
     */
    @Serializable
    data class AddEditDeworming(
        val patientId: Long,
        val dewormingId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a dentistry check.
     */
    @Serializable
    data class AddEditDentistry(
        val patientId: Long,
        val dentistryId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a lameness evaluation.
     */
    @Serializable
    data class AddEditLameness(
        val patientId: Long,
        val lamenessId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a surgery.
     */
    @Serializable
    data class AddEditSurgery(
        val patientId: Long,
        val surgeryId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a medication.
     */
    @Serializable
    data class AddEditMedication(
        val patientId: Long,
        val medicationId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a lab result.
     */
    @Serializable
    data class AddEditLabResult(
        val patientId: Long,
        val labResultId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing an imaging study.
     */
    @Serializable
    data class AddEditImaging(
        val patientId: Long,
        val imagingId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a farrier visit.
     */
    @Serializable
    data class AddEditFarrierVisit(
        val patientId: Long,
        val farrierVisitId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a reproduction event.
     */
    @Serializable
    data class AddEditReproductionEvent(
        val patientId: Long,
        val reproductionEventId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a reproductive ultrasound.
     */
    @Serializable
    data class AddEditUltrasound(
        val patientId: Long,
        val ultrasoundId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a gestation record.
     */
    @Serializable
    data class AddEditGestation(
        val patientId: Long,
        val gestationId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a reproductive medication.
     */
    @Serializable
    data class AddEditReproMed(
        val patientId: Long,
        val reproMedId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a controlled substance.
     */
    @Serializable
    data class AddEditControlledSubstance(
        val patientId: Long,
        val substanceId: Long? = null,
    ) : Route

    /**
     * The route for adding or editing a custom reminder.
     */
    @Serializable
    data class AddEditCustomReminder(
        val patientId: Long,
        val reminderId: Long? = null,
    ) : Route

    /**
     * The route for displaying the custom reminders of a patient.
     */
    @Serializable
    data class CustomReminderList(
        val patientId: Long,
    ) : Route
}
