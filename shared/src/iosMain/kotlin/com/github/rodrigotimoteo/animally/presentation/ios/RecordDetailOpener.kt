@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.bridge.NativeCancellable
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.di.infra.IosEditStores
import com.github.rodrigotimoteo.animally.di.infra.IosEditStoresFiles
import com.github.rodrigotimoteo.animally.di.infra.IosEditStoresMedical
import com.github.rodrigotimoteo.animally.di.infra.IosEditStoresRepro
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the read-only record detail.
 *
 * @property rows The ready-to-render field rows, or `null` while loading or
 *   when the record does not exist.
 * @property isLoading Whether the underlying form is still loading.
 */
@ObjCName("RecordDetailState")
data class RecordDetailState(
    val rows: List<RecordDetailRow>? = null,
    val isLoading: Boolean = false,
)

/**
 * Typed description of the editor push destination for a loaded record.
 * [typeName] is the stable [RecordType.name] discriminator the Swift side
 * maps to its navigation route.
 */
@ObjCName("RecordEditRouteDescriptor")
data class RecordEditRouteDescriptor(
    val typeName: String,
    val patientId: Long,
    val recordId: Long,
)

/**
 * Everything the read-only record detail screen needs for one record:
 * display title, ready field-row flow, and the edit-route descriptor.
 *
 * Backed by the matching Kotlin edit store; call [dispose] when the view
 * goes away to stop the flow collection.
 */
@ObjCName("RecordDetailHandle")
class RecordDetailHandle(
    val title: String,
    val editRoute: RecordEditRouteDescriptor?,
    val state: NativeFlow<RecordDetailState>,
    private val scope: CoroutineScope,
    private val subscriptions: List<NativeCancellable>,
) {
    /** Stops collecting the backing store flow. Safe to call more than once. */
    fun dispose() {
        subscriptions.forEach { it.cancel() }
        scope.cancel()
    }
}

/** Parses both deep-link vocabularies: uppercase wire names and display names. */
private fun parseRecordType(raw: String): RecordType? =
    RecordType.fromWireName(raw)
        ?: RecordType.fromDisplayName(raw)
        ?: RecordType.entries.firstOrNull {
            it.wireName.equals(raw, ignoreCase = true) || it.displayName.equals(raw, ignoreCase = true)
        }

/**
 * Nav-bar title. Preserves the historical Swift normalization exactly: only
 * multi-word underscore wire names ("LAB_RESULT") resolve to their display
 * name; every other input passes through unchanged.
 */
private fun titleFor(
    raw: String,
    type: RecordType?,
): String {
    val resolved = type ?: return raw
    val isUnderscoreWireName = '_' in resolved.wireName && resolved.wireName.equals(raw, ignoreCase = true)
    return if (isUnderscoreWireName) resolved.displayName else raw
}

/** Edit-route descriptor for types that have an iOS editor; null otherwise. */
private fun editRouteFor(
    type: RecordType,
    patientId: Long?,
    recordId: Long,
): RecordEditRouteDescriptor? {
    val pid = patientId ?: return null
    return when (type) {
        RecordType.Consultation,
        RecordType.Weight,
        RecordType.Vaccination,
        RecordType.Deworming,
        RecordType.Dentistry,
        RecordType.FarrierVisit,
        RecordType.Lameness,
        RecordType.Surgery,
        RecordType.Medication,
        RecordType.ControlledSubstance,
        RecordType.LabResult,
        RecordType.Imaging,
        RecordType.ReproductionEvent,
        RecordType.Ultrasound,
        RecordType.Gestation,
        RecordType.ReproMedication,
        RecordType.EmbryoTransfer,
        RecordType.Icsi,
        -> RecordEditRouteDescriptor(typeName = type.name, patientId = pid, recordId = recordId)

        RecordType.Anamnese,
        RecordType.CustomReminder,
        RecordType.Owner,
        RecordType.Patient,
        -> null
    }
}

private fun notFoundFlow(scope: CoroutineScope): NativeFlow<RecordDetailState> =
    NativeFlow(MutableStateFlow(RecordDetailState(rows = null, isLoading = false)), scope)

/**
 * Re-emits the typed store state as a [RecordDetailState] flow: decodes the
 * loaded form into ready field rows and carries the loading flag.
 */
private fun <S : Any> bind(
    source: NativeFlow<S>,
    scope: CoroutineScope,
    isLoadingOf: (S) -> Boolean,
    rowsOf: (S) -> List<RecordDetailRow>?,
): DetailBinding {
    val mapped = MutableStateFlow(decode(source.current, isLoadingOf, rowsOf))
    val subscription = source.subscribe { mapped.value = decode(it, isLoadingOf, rowsOf) }
    return DetailBinding(NativeFlow(mapped, scope), listOf(subscription))
}

private fun <S : Any> decode(
    state: S,
    isLoadingOf: (S) -> Boolean,
    rowsOf: (S) -> List<RecordDetailRow>?,
): RecordDetailState = RecordDetailState(rows = rowsOf(state), isLoading = isLoadingOf(state))

/** A bound store: the unified state flow plus the subscriptions to cancel. */
private class DetailBinding(
    val flow: NativeFlow<RecordDetailState>,
    val subscriptions: List<NativeCancellable>,
)

/**
 * Single source of truth for opening the read-only record detail.
 *
 * Resolves the record-type string carried by deep links (search wire names
 * like "LAB_RESULT" or timeline/tab display names like "Lab Result") against
 * the [RecordType] enum and dispatches ONCE over the typed enum to bind the
 * matching edit store and field-row builder. Adding a record type therefore
 * requires exactly one exhaustive `when` here — the compiler enforces it —
 * instead of parallel string switches on the Swift side.
 */
@ObjCName("RecordDetailOpener")
object RecordDetailOpener {
    /**
     * Opens the detail handle for the record identified by
     * [recordTypeWireName], [recordId], and [patientId]. Never returns null:
     * unknown types yield a handle whose state renders the not-found
     * placeholder, mirroring the previous Swift behavior.
     */
    fun openDetail(
        recordTypeWireName: String,
        recordId: Long,
        patientId: Long?,
    ): RecordDetailHandle {
        val type = parseRecordType(recordTypeWireName)
        val title = titleFor(recordTypeWireName, type)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val binding =
            if (type != null && patientId != null) {
                stateFor(type, patientId, recordId, scope)
            } else {
                null
            }
        return RecordDetailHandle(
            title = title,
            editRoute = type?.let { editRouteFor(it, patientId, recordId) },
            state = binding?.flow ?: notFoundFlow(scope),
            scope = scope,
            subscriptions = binding?.subscriptions ?: emptyList(),
        )
    }

    private fun stateFor(
        type: RecordType,
        patientId: Long,
        recordId: Long,
        scope: CoroutineScope,
    ): DetailBinding? =
        when (type) {
            RecordType.Consultation,
            RecordType.Weight,
            RecordType.Vaccination,
            RecordType.Deworming,
            RecordType.Dentistry,
            RecordType.FarrierVisit,
            -> basicGroup(type, patientId, recordId, scope)

            RecordType.Lameness,
            RecordType.Surgery,
            RecordType.Medication,
            RecordType.ControlledSubstance,
            RecordType.LabResult,
            RecordType.Imaging,
            -> medicalGroup(type, patientId, recordId, scope)

            RecordType.ReproductionEvent,
            RecordType.Ultrasound,
            RecordType.Gestation,
            RecordType.ReproMedication,
            RecordType.EmbryoTransfer,
            RecordType.Icsi,
            -> reproGroup(type, patientId, recordId, scope)

            RecordType.Anamnese,
            RecordType.CustomReminder,
            RecordType.Owner,
            RecordType.Patient,
            -> null
        }

    private fun basicGroup(
        type: RecordType,
        patientId: Long,
        recordId: Long,
        scope: CoroutineScope,
    ): DetailBinding? =
        when (type) {
            RecordType.Consultation ->
                bind(
                    IosEditStores.consultationEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::consultationRows) },
                )
            RecordType.Weight ->
                bind(
                    IosEditStores.weightEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::weightRows) },
                )
            RecordType.Vaccination ->
                bind(
                    IosEditStores.vaccinationEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::vaccinationRows) },
                )
            RecordType.Deworming ->
                bind(
                    IosEditStores.dewormingEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::dewormingRows) },
                )
            RecordType.Dentistry ->
                bind(
                    IosEditStores.dentistryEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::dentistryRows) },
                )
            RecordType.FarrierVisit ->
                bind(
                    IosEditStores.farrierVisitEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::farrierRows) },
                )
            else -> null
        }

    private fun medicalGroup(
        type: RecordType,
        patientId: Long,
        recordId: Long,
        scope: CoroutineScope,
    ): DetailBinding? =
        when (type) {
            RecordType.Lameness ->
                bind(
                    IosEditStoresMedical.lamenessEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::lamenessRows) },
                )
            RecordType.Surgery ->
                bind(
                    IosEditStoresMedical.surgeryEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::surgeryRows) },
                )
            RecordType.Medication ->
                bind(
                    IosEditStoresMedical.medicationEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::medicationRows) },
                )
            RecordType.ControlledSubstance ->
                bind(
                    IosEditStoresMedical.substanceEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::substanceRows) },
                )
            RecordType.LabResult ->
                bind(
                    IosEditStoresMedical.labResultEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::labResultRows) },
                )
            RecordType.Imaging ->
                bind(
                    IosEditStoresFiles.imagingEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::imagingRows) },
                )
            else -> null
        }

    private fun reproGroup(
        type: RecordType,
        patientId: Long,
        recordId: Long,
        scope: CoroutineScope,
    ): DetailBinding? =
        when (type) {
            RecordType.ReproductionEvent ->
                bind(
                    IosEditStoresRepro.reproductionEventEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::reproductionRows) },
                )
            RecordType.Ultrasound ->
                bind(
                    IosEditStoresRepro.ultrasoundEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::ultrasoundRows) },
                )
            RecordType.Gestation ->
                bind(
                    IosEditStoresRepro.gestationEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::gestationRows) },
                )
            RecordType.ReproMedication ->
                bind(
                    IosEditStoresRepro.reproMedicationEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::reproMedicationRows) },
                )
            RecordType.EmbryoTransfer ->
                bind(
                    IosEditStoresRepro.embryoTransferEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::embryoTransferRows) },
                )
            RecordType.Icsi ->
                bind(
                    IosEditStoresRepro.icsiEditStore(patientId, recordId).state,
                    scope,
                    isLoadingOf = { it.form?.isLoading == true },
                    rowsOf = { it.form?.let(::icsiRows) },
                )
            else -> null
        }
}
