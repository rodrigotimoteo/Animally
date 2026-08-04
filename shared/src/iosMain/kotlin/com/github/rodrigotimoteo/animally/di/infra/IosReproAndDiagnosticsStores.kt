@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingListViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.GestationListStore
import com.github.rodrigotimoteo.animally.presentation.ios.ImagingListStore
import com.github.rodrigotimoteo.animally.presentation.ios.LabResultListStore
import com.github.rodrigotimoteo.animally.presentation.ios.ReproMedicationListStore
import com.github.rodrigotimoteo.animally.presentation.ios.ReproductionListStore
import com.github.rodrigotimoteo.animally.presentation.ios.UltrasoundListStore
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultListViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventListViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the reproduction and diagnostics list stores
 * embedded in the patient detail screen. Split from [IosAppBridge] to stay
 * within the detekt function-count budget.
 */
@ObjCName("IosReproAndDiagnosticsStores")
object IosReproAndDiagnosticsStores {
    /** Returns a store exposing the reproduction-event list for the patient with [patientId]. */
    fun reproductionListStore(patientId: Long): ReproductionListStore {
        val viewModel: ReproductionEventListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return ReproductionListStore(viewModel)
    }

    /** Returns a store exposing the ultrasound list for the patient with [patientId]. */
    fun ultrasoundListStore(patientId: Long): UltrasoundListStore {
        val viewModel: UltrasoundListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return UltrasoundListStore(viewModel)
    }

    /** Returns a store exposing the gestation list for the patient with [patientId]. */
    fun gestationListStore(patientId: Long): GestationListStore {
        val viewModel: GestationListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return GestationListStore(viewModel)
    }

    /** Returns a store exposing the reproduction-medication list for the patient with [patientId]. */
    fun reproMedicationListStore(patientId: Long): ReproMedicationListStore {
        val viewModel: ReproMedicationListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return ReproMedicationListStore(viewModel)
    }

    /** Returns a store exposing the lab-result list for the patient with [patientId]. */
    fun labResultListStore(patientId: Long): LabResultListStore {
        val viewModel: LabResultListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return LabResultListStore(viewModel)
    }

    /** Returns a store exposing the imaging list for the patient with [patientId]. */
    fun imagingListStore(patientId: Long): ImagingListStore {
        val viewModel: ImagingListViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return ImagingListStore(viewModel)
    }
}
