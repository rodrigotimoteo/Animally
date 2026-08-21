@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.ConsultationEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.DentistryEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.DewormingEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.FarrierVisitEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.VaccinationEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.WeightEditStore
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightEditViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the medical-record add/edit form stores opened
 * from the patient detail "+" menu. Split from [IosRecordStores] to stay
 * within the detekt function-count budget.
 */
@ObjCName("IosEditStores")
object IosEditStores {
    /**
     * Returns a store exposing the add/edit form for the weight entry with
     * [weightId], or a new-entry form when `null`, for the patient with
     * [patientId].
     */
    fun weightEditStore(
        patientId: Long,
        weightId: Long? = null,
    ): WeightEditStore {
        val viewModel: WeightEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, weightId) }
        return WeightEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the vaccination with
     * [vaccinationId], or a new-vaccination form when `null`, for the patient
     * with [patientId].
     */
    fun vaccinationEditStore(
        patientId: Long,
        vaccinationId: Long? = null,
    ): VaccinationEditStore {
        val viewModel: VaccinationEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, vaccinationId) }
        return VaccinationEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the deworming with
     * [dewormingId], or a new-deworming form when `null`, for the patient with
     * [patientId].
     */
    fun dewormingEditStore(
        patientId: Long,
        dewormingId: Long? = null,
    ): DewormingEditStore {
        val viewModel: DewormingEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, dewormingId) }
        return DewormingEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the consultation with
     * [consultationId], or a new-consultation form when `null`, for the patient
     * with [patientId].
     */
    fun consultationEditStore(
        patientId: Long,
        consultationId: Long? = null,
    ): ConsultationEditStore {
        val viewModel: ConsultationEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, consultationId) }
        return ConsultationEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the dentistry record with
     * [dentistryId], or a new-record form when `null`, for the patient with
     * [patientId].
     */
    fun dentistryEditStore(
        patientId: Long,
        dentistryId: Long? = null,
    ): DentistryEditStore {
        val viewModel: DentistryEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, dentistryId) }
        return DentistryEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the farrier visit with
     * [farrierVisitId], or a new-visit form when `null`, for the patient with
     * [patientId].
     */
    fun farrierVisitEditStore(
        patientId: Long,
        farrierVisitId: Long? = null,
    ): FarrierVisitEditStore {
        val viewModel: FarrierVisitEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, farrierVisitId) }
        return FarrierVisitEditStore(viewModel)
    }
}
