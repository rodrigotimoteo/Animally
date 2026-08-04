@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryListViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.ConsultationListStore
import com.github.rodrigotimoteo.animally.presentation.ios.DentistryListStore
import com.github.rodrigotimoteo.animally.presentation.ios.DewormingListStore
import com.github.rodrigotimoteo.animally.presentation.ios.FarrierVisitListStore
import com.github.rodrigotimoteo.animally.presentation.ios.LamenessListStore
import com.github.rodrigotimoteo.animally.presentation.ios.MedicationListStore
import com.github.rodrigotimoteo.animally.presentation.ios.SubstanceListStore
import com.github.rodrigotimoteo.animally.presentation.ios.SurgeryListStore
import com.github.rodrigotimoteo.animally.presentation.ios.VaccinationListStore
import com.github.rodrigotimoteo.animally.presentation.ios.WeightListStore
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationListViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryListViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the medical-record list stores embedded in the
 * patient detail screen. Split from [IosAppBridge] to stay within the detekt
 * function-count budget.
 */
@ObjCName("IosRecordStores")
object IosRecordStores {
    /** Returns a store exposing the consultation list for the patient with [patientId]. */
    fun consultationListStore(patientId: Long): ConsultationListStore {
        val viewModel: ConsultationListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return ConsultationListStore(viewModel)
    }

    /** Returns a store exposing the vaccination list for the patient with [patientId]. */
    fun vaccinationListStore(patientId: Long): VaccinationListStore {
        val viewModel: VaccinationListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return VaccinationListStore(viewModel)
    }

    /** Returns a store exposing the deworming list for the patient with [patientId]. */
    fun dewormingListStore(patientId: Long): DewormingListStore {
        val viewModel: DewormingListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return DewormingListStore(viewModel)
    }

    /** Returns a store exposing the dentistry list for the patient with [patientId]. */
    fun dentistryListStore(patientId: Long): DentistryListStore {
        val viewModel: DentistryListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return DentistryListStore(viewModel)
    }

    /** Returns a store exposing the farrier visit list for the patient with [patientId]. */
    fun farrierVisitListStore(patientId: Long): FarrierVisitListStore {
        val viewModel: FarrierVisitListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return FarrierVisitListStore(viewModel)
    }

    /** Returns a store exposing the lameness list for the patient with [patientId]. */
    fun lamenessListStore(patientId: Long): LamenessListStore {
        val viewModel: LamenessListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return LamenessListStore(viewModel)
    }

    /** Returns a store exposing the surgery list for the patient with [patientId]. */
    fun surgeryListStore(patientId: Long): SurgeryListStore {
        val viewModel: SurgeryListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return SurgeryListStore(viewModel)
    }

    /** Returns a store exposing the medication list for the patient with [patientId]. */
    fun medicationListStore(patientId: Long): MedicationListStore {
        val viewModel: MedicationListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return MedicationListStore(viewModel)
    }

    /** Returns a store exposing the controlled-substance list for the patient with [patientId]. */
    fun substanceListStore(patientId: Long): SubstanceListStore {
        val viewModel: ControlledSubstanceListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return SubstanceListStore(viewModel)
    }

    /** Returns a store exposing the weight list for the patient with [patientId]. */
    fun weightListStore(patientId: Long): WeightListStore {
        val viewModel: WeightListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return WeightListStore(viewModel)
    }
}
