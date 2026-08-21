@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.CustomReminderEditStore
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the care add/edit form stores opened from the
 * patient detail "+" menu. Split from [IosEditStores] to stay within the
 * detekt function-count budget.
 */
@ObjCName("IosEditStoresCare")
object IosEditStoresCare {
    /**
     * Returns a store exposing the add/edit form for the custom reminder with
     * [reminderId], or a new-reminder form when `null`, for the patient with
     * [patientId].
     */
    fun customReminderEditStore(
        patientId: Long,
        reminderId: Long? = null,
    ): CustomReminderEditStore {
        val viewModel: CustomReminderEditViewModel = IosAppBridge.koin.get { parametersOf(patientId, reminderId) }
        return CustomReminderEditStore(viewModel)
    }
}
