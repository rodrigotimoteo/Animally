@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.ImagingEditStore
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the files/diagnostics add/edit form stores opened
 * from the patient detail "+" menu. Split from [IosEditStores] to stay within
 * the detekt function-count budget.
 */
@ObjCName("IosEditStoresFiles")
object IosEditStoresFiles {
    /**
     * Returns a store exposing the add/edit form for the imaging record with
     * [imagingId], or a new-record form when `null`, for the patient with
     * [patientId].
     */
    fun imagingEditStore(
        patientId: Long,
        imagingId: Long? = null,
    ): ImagingEditStore {
        val viewModel: ImagingEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, imagingId) }
        return ImagingEditStore(viewModel)
    }
}
