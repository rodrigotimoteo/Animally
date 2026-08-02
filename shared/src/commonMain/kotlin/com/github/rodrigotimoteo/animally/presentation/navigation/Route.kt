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
}
