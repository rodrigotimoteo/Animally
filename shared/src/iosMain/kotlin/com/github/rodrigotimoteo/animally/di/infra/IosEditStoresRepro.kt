@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferEditViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.EmbryoTransferEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.GestationEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.IcsiEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.ReproMedicationEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.ReproductionEventEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.UltrasoundEditStore
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundEditViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the reproduction add/edit form stores opened from
 * the patient detail "+" menu. Split from [IosEditStores] to stay within the
 * detekt function-count budget.
 */
@ObjCName("IosEditStoresRepro")
object IosEditStoresRepro {
    /**
     * Returns a store exposing the add/edit form for the reproduction event
     * with [reproductionEventId], or a new-event form when `null`, for the
     * patient with [patientId].
     */
    fun reproductionEventEditStore(
        patientId: Long,
        reproductionEventId: Long? = null,
    ): ReproductionEventEditStore {
        val viewModel: ReproductionEventEditViewModel =
            IosAppBridge.koin.get { parametersOf(patientId, reproductionEventId) }
        return ReproductionEventEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the reproductive
     * ultrasound with [ultrasoundId], or a new-ultrasound form when `null`,
     * for the patient with [patientId].
     */
    fun ultrasoundEditStore(
        patientId: Long,
        ultrasoundId: Long? = null,
    ): UltrasoundEditStore {
        val viewModel: UltrasoundEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, ultrasoundId) }
        return UltrasoundEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the embryo transfer record with
     * [embryoTransferId], or a new-record form when `null`, for the patient with
     * [patientId].
     */
    fun embryoTransferEditStore(
        patientId: Long,
        embryoTransferId: Long? = null,
    ): EmbryoTransferEditStore {
        val viewModel: EmbryoTransferEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, embryoTransferId) }
        return EmbryoTransferEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the ICSI record with
     * [icsiId], or a new-record form when `null`, for the patient with
     * [patientId].
     */
    fun icsiEditStore(
        patientId: Long,
        icsiId: Long? = null,
    ): IcsiEditStore {
        val viewModel: IcsiEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, icsiId) }
        return IcsiEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the gestation record with
     * [gestationId], or a new-record form when `null`, for the patient with
     * [patientId].
     */
    fun gestationEditStore(
        patientId: Long,
        gestationId: Long? = null,
    ): GestationEditStore {
        val viewModel: GestationEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, gestationId) }
        return GestationEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the reproduction
     * medication with [reproMedicationId], or a new-medication form when
     * `null`, for the patient with [patientId].
     */
    fun reproMedicationEditStore(
        patientId: Long,
        reproMedicationId: Long? = null,
    ): ReproMedicationEditStore {
        val viewModel: ReproMedicationEditViewModel =
            IosAppBridge.koin.get { parametersOf(patientId, reproMedicationId) }
        return ReproMedicationEditStore(viewModel)
    }
}
