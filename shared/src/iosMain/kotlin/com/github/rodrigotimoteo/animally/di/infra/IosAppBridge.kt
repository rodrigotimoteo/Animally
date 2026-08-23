@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.ios.OwnerDetailStore
import com.github.rodrigotimoteo.animally.presentation.ios.OwnerEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.OwnerListStore
import com.github.rodrigotimoteo.animally.presentation.ios.PatientDetailStore
import com.github.rodrigotimoteo.animally.presentation.ios.PatientEditStore
import com.github.rodrigotimoteo.animally.presentation.ios.PatientListStore
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListViewModel
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Entry point for the native iOS UI.
 *
 * Boots Koin via [initKoin] and resolves the stores that SwiftUI consumes.
 * The record-list stores live on [IosRecordStores] and the settings-related
 * stores on [IosSettingsStores].
 */
@ObjCName("IosAppBridge")
object IosAppBridge {
    /** The active Koin instance, shared with the other store bridge objects. */
    internal lateinit var koin: Koin
        private set

    /** Boots Koin with the production modules and stores the instance. */
    fun start() {
        koin = initKoin().koin
        // Heal the search index only when its layout version changed or the
        // index is empty; every other launch takes the one-read fast path.
        koin.get<ISearchRepository>().reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
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

    /** Returns a store exposing the owner list. */
    fun ownerListStore(): OwnerListStore {
        val viewModel: OwnerListViewModel = koin.get()
        return OwnerListStore(viewModel)
    }

    /** Returns a store exposing the detail screen for the owner with [ownerId]. */
    fun ownerDetailStore(ownerId: Long): OwnerDetailStore {
        val viewModel: OwnerDetailViewModel = koin.get { parametersOf(ownerId) }
        return OwnerDetailStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the owner with [ownerId],
     * or a new-owner form when `null`.
     */
    fun ownerEditStore(ownerId: Long?): OwnerEditStore {
        val viewModel: OwnerEditViewModel = koin.get { parametersOf(ownerId) }
        return OwnerEditStore(viewModel)
    }

    /**
     * Returns a store exposing the add/edit form for the patient with
     * [patientId], or a new-patient form when `null`, with [initialOwnerId]
     * pre-selected on the form when creating a new patient.
     */
    fun patientEditStore(
        patientId: Long?,
        initialOwnerId: Long? = null,
    ): PatientEditStore {
        val viewModel: PatientEditViewModel = koin.get { parametersOf(patientId, initialOwnerId) }
        return PatientEditStore(viewModel)
    }
}
