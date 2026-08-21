@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.AnamneseEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.LabResultEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.LamenessEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.MedicationEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.SubstanceEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.SurgeryEditStore
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultEditViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessEditViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceEditViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryEditViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the medical add/edit form stores opened from the
 * patient detail "+" menu. Split from [IosEditStores] to stay within the
 * detekt function-count budget.
 */
@ObjCName("IosEditStoresMedical")
object IosEditStoresMedical {
    /**
     * Returns a store exposing the anamnese form for the patient with
     * [patientId]. Anamnese is 1:1 with the patient; [anamneseId] resolves to
     * the same single record.
     */
    fun anamneseEditStore(
        patientId: Long,
        anamneseId: Long? = null,
    ): AnamneseEditStore {
        val viewModel: AnamneseViewModel = IosAppBridge.koin.get { parametersOf(patientId, anamneseId) }
        return AnamneseEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the lameness evaluation
     * with [lamenessId], or a new-evaluation form when `null`, for the patient
     * with [patientId].
     */
    fun lamenessEditStore(
        patientId: Long,
        lamenessId: Long? = null,
    ): LamenessEditStore {
        val viewModel: LamenessEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, lamenessId) }
        return LamenessEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the surgery with
     * [surgeryId], or a new-surgery form when `null`, for the patient with
     * [patientId].
     */
    fun surgeryEditStore(
        patientId: Long,
        surgeryId: Long? = null,
    ): SurgeryEditStore {
        val viewModel: SurgeryEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, surgeryId) }
        return SurgeryEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the medication with
     * [medicationId], or a new-medication form when `null`, for the patient
     * with [patientId].
     */
    fun medicationEditStore(
        patientId: Long,
        medicationId: Long? = null,
    ): MedicationEditStore {
        val viewModel: MedicationEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, medicationId) }
        return MedicationEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the controlled-substance
     * record with [substanceId], or a new-record form when `null`, for the
     * patient with [patientId].
     */
    fun substanceEditStore(
        patientId: Long,
        substanceId: Long? = null,
    ): SubstanceEditStore {
        val viewModel: ControlledSubstanceEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, substanceId) }
        return SubstanceEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the lab result with
     * [labResultId], or a new-result form when `null`, for the patient with
     * [patientId].
     */
    fun labResultEditStore(
        patientId: Long,
        labResultId: Long? = null,
    ): LabResultEditStore {
        val viewModel: LabResultEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, labResultId) }
        return LabResultEditStore(viewModel)
    }
}
