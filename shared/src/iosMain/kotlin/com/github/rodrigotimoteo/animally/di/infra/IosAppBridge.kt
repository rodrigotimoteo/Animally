@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.ios.PatientDetailStore
import com.github.rodrigotimoteo.animally.presentation.ios.PatientListStore
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Entry point for the native iOS UI.
 *
 * Boots Koin via [initKoin] and resolves the stores that SwiftUI consumes.
 */
@ObjCName("IosAppBridge")
object IosAppBridge {
    private lateinit var koin: Koin

    /** Boots Koin with the production modules and stores the instance. */
    fun start() {
        koin = initKoin().koin
    }

    /**
     * Test hook: installs a Koin instance built with test modules instead of
     * the production ones.
     */
    internal fun start(koin: Koin) {
        this.koin = koin
    }

    /** Returns a store exposing the patient list. */
    fun patientListStore(): PatientListStore {
        val viewModel: PatientListViewModel = koin.get()
        return PatientListStore(viewModel)
    }

    /** Returns a store exposing the detail screen for the patient with [patientId]. */
    fun patientDetailStore(patientId: Long): PatientDetailStore {
        val viewModel: PatientDetailViewModel = koin.get { parametersOf(patientId) }
        return PatientDetailStore(viewModel)
    }
}
