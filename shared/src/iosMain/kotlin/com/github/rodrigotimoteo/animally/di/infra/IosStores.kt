@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Unified Swift-facing entry point for iOS store factories.
 *
 * The per-entity factories remain split across [IosEditStores],
 * [IosEditStoresMedical], [IosEditStoresRepro], [IosEditStoresCare],
 * [IosEditStoresFiles], [IosRecordStores], [IosMedicalRecordStores],
 * [IosPreventiveRecordStores], [IosCareStores], [IosReproAndDiagnosticsStores],
 * and [IosSettingsStores] to stay within the Detekt `TooManyFunctions`
 * budget (threshold 15). [IosStores] provides a single discoverable
 * namespace that delegates to those domain-grouped modules, reducing
 * fragmentation without reintroducing a god object.
 *
 * Swift usage:
 * ```
 * IosStores.edit.weightEditStore(patientId: 1, weightId: nil)
 * IosStores.medical.lamenessEditStore(patientId: 1, lamenessId: nil)
 * IosStores.repro.embryoTransferEditStore(patientId: 1, embryoTransferId: nil)
 * IosStores.records.weightListStore(patientId: 1)
 * IosStores.settings.settingsStore()
 * ```
 *
 * Existing per-domain objects remain available for backward compatibility.
 */
@ObjCName("IosStores")
object IosStores {
    /** Core medical-record edit factories (weight, vaccination, deworming, consultation, dentistry, farrier). */
    val edit: IosEditStores get() = IosEditStores

    /** Medical edit factories (anamnese, lameness, surgery, medication, substance, lab result). */
    val medical: IosEditStoresMedical get() = IosEditStoresMedical

    /**
     * Reproduction edit factories (reproduction event, ultrasound, embryo transfer,
     * ICSI, gestation, repro medication).
     */
    val repro: IosEditStoresRepro get() = IosEditStoresRepro

    /** Care edit factories (custom reminder). */
    val care: IosEditStoresCare get() = IosEditStoresCare

    /** Files/diagnostics edit factories (imaging). */
    val files: IosEditStoresFiles get() = IosEditStoresFiles

    /**
     * Patient-detail list factories (consultation, vaccination, deworming, dentistry,
     * farrier, lameness, surgery, medication, substance, weight, etc.).
     */
    val records: IosRecordStores get() = IosRecordStores

    /** Reproduction and diagnostics list factories. */
    val reproAndDiagnostics: IosReproAndDiagnosticsStores
        get() = IosReproAndDiagnosticsStores

    /** Settings and global list factories. */
    val settings: IosSettingsStores get() = IosSettingsStores
}
