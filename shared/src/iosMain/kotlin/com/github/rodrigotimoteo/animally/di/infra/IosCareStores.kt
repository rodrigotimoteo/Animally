@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.care.UpcomingCareViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.UpcomingCareStore
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/** Swift-facing factory for the patient-detail upcoming-care panel. */
@ObjCName("IosCareStores")
object IosCareStores {
    /** Returns the upcoming-care store for [patientId]. */
    fun upcomingCareStore(patientId: Long): UpcomingCareStore {
        val viewModel: UpcomingCareViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return UpcomingCareStore(viewModel)
    }
}
