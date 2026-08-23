import Foundation
import Shared
import SwiftUI

/// Navigation payload for the read-only record detail screen.
struct RecordDetailNav: Identifiable, Hashable {
    struct FieldRow: Identifiable, Hashable {
        let label: String
        let value: String
        var id: String { label }

        init(label: String, value: String) {
            self.label = label
            self.value = value
        }
    }

    let id = UUID()
    let title: String
    let displayType: String
    let patientId: Int64
    let recordId: Int64
    let fields: [FieldRow]
}

/// Hashable navigation payload for opening the detail by record identity:
/// the view loads the record itself instead of receiving eager field rows.
struct RecordDetailKey: Hashable {
    let displayType: String
    let patientId: Int64
    let recordId: Int64
}

/// Read-only view of everything inside one record. "Edit" opens the
/// prefilled form; back returns to the patient page.
///
/// Two modes:
/// - Eager (`init(nav:onEdit:)`): tab views already hold the entity and pass
///   prebuilt field rows.
/// - Id-loaded (`init(displayType:patientId:recordId:onEdit:)`): timeline and
///   search only know the record identity; the matching Kotlin edit store is
///   instantiated here and its loaded form state supplies the field rows.
struct RecordDetailView: View {
    private enum Source {
        case eager(RecordDetailNav)
        case byId(RecordDetailKey)
    }

    private let source: Source
    private let title: String
    private let onEdit: () -> Void

    init(
        nav: RecordDetailNav,
        onEdit: @escaping () -> Void
    ) {
        source = .eager(nav)
        title = nav.title
        self.onEdit = onEdit
    }

    init(
        displayType: String,
        patientId: Int64,
        recordId: Int64,
        onEdit: @escaping () -> Void
    ) {
        source = .byId(RecordDetailKey(
            displayType: displayType,
            patientId: patientId,
            recordId: recordId
        ))
        title = displayType
        self.onEdit = onEdit
    }

    @ViewBuilder
    var body: some View {
        switch source {
        case .eager(let nav):
            eagerBody(nav)
        case .byId(let key):
            IdLoadedRecordDetailView(key: key, fallbackTitle: title, onEdit: onEdit)
        }
    }

    private func eagerBody(_ nav: RecordDetailNav) -> some View {
        List {
            Section {
                ForEach(nav.fields) { field in
                    FieldCell(field: field)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(nav.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // Consistent with the id-loaded mode: no Edit until field rows
            // exist (eager payloads are built up front, so an empty list
            // means the source data was not ready).
            ToolbarItem(placement: .topBarTrailing) {
                Button("Edit", action: onEdit)
                    .font(.subheadline.weight(.semibold))
                    .disabled(nav.fields.isEmpty)
            }
        }
    }
}

private struct FieldCell: View {
    let field: RecordDetailNav.FieldRow

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(field.label)
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
            Text(field.value)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
        }
        .padding(.vertical, 3)
    }
}

/// Id-loaded mode: instantiates the Kotlin-backed edit store for the record,
/// subscribes to its state, and renders the loaded form as field rows.
private struct IdLoadedRecordDetailView: View {
    let key: RecordDetailKey
    let fallbackTitle: String
    let onEdit: () -> Void

    @StateObject private var observer: RecordDetailObserver

    init(
        key: RecordDetailKey,
        fallbackTitle: String,
        onEdit: @escaping () -> Void
    ) {
        self.key = key
        self.fallbackTitle = fallbackTitle
        self.onEdit = onEdit
        _observer = StateObject(wrappedValue: RecordDetailObserver(key: key))
    }

    var body: some View {
        Group {
            if observer.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading \(fallbackTitle)…")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let fields = observer.fields {
                List {
                    Section {
                        ForEach(fields) { field in
                            FieldCell(field: field)
                        }
                    }
                }
                .listStyle(.insetGrouped)
            } else {
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 64))
                        .foregroundStyle(Theme.amber)
                    Text("Record not found")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(Theme.textPrimary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(observer.title ?? fallbackTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Edit", action: onEdit)
                    .font(.subheadline.weight(.semibold))
                    .disabled(observer.fields == nil)
            }
        }
    }
}

/// Subscribes to the matching edit store and converts each emitted store
/// state into ready-to-render field rows (or loading / not-found markers).
@MainActor
final class RecordDetailObserver: ObservableObject {
    @Published private(set) var fields: [RecordDetailNav.FieldRow]?
    @Published private(set) var isLoading = true
    @Published private(set) var title: String?

    private var cancellable: NativeCancellable?

    /// Type-erased view over one Kotlin edit store: the initial state snapshot
    /// plus a subscription that re-emits states as untyped objects.
    private struct StoreBox {
        let initial: AnyObject
        let subscribe: (@escaping (AnyObject) -> Void) -> NativeCancellable
    }

    init(key: RecordDetailKey) {
        // Timeline entries carry display names ("Lab Result"); search results
        // carry wire names ("LAB_RESULT"). Normalize once here.
        let displayType = Self.normalizedDisplayType(key.displayType)
        title = displayType
        guard let box = Self.storeBox(for: displayType, patientId: key.patientId, recordId: key.recordId) else {
            isLoading = false
            return
        }
        apply(state: box.initial, displayType: displayType)
        cancellable = box.subscribe { [weak self] state in
            Task { @MainActor in
                self?.apply(state: state, displayType: displayType)
            }
        }
    }

    private func apply(state: AnyObject, displayType: String) {
        fields = Self.fields(from: state, displayType: displayType)
        isLoading = Self.formIsLoading(state, displayType: displayType)
        if fields?.isEmpty == true, !isLoading {
            fields = nil
        }
    }

    private static func normalizedDisplayType(_ raw: String) -> String {
        switch raw.uppercased() {
        case "LAB_RESULT": return "Lab Result"
        case "FARRIER_VISIT": return "Farrier"
        case "REPRODUCTION_EVENT": return "Reproduction"
        case "REPRO_MEDICATION": return "Repro Medication"
        case "CONTROLLED_SUBSTANCE": return "Controlled Substance"
        case "EMBRYO_TRANSFER": return "Embryo Transfer"
        default: return raw
        }
    }

    deinit {
        cancellable?.cancel()
    }

    private static func formIsLoading(_ state: AnyObject, displayType: String) -> Bool {
        switch displayType {
        case "Consultation": return (state as? ConsultationEditStoreState)?.form?.isLoading == true
        case "Weight": return (state as? WeightEditStoreState)?.form?.isLoading == true
        case "Vaccination": return (state as? VaccinationEditStoreState)?.form?.isLoading == true
        case "Deworming": return (state as? DewormingEditStoreState)?.form?.isLoading == true
        case "Dentistry": return (state as? DentistryEditStoreState)?.form?.isLoading == true
        case "Farrier": return (state as? FarrierVisitEditStoreState)?.form?.isLoading == true
        case "Lameness": return (state as? LamenessEditStoreState)?.form?.isLoading == true
        case "Surgery": return (state as? SurgeryEditStoreState)?.form?.isLoading == true
        case "Medication": return (state as? MedicationEditStoreState)?.form?.isLoading == true
        case "Controlled Substance": return (state as? SubstanceEditStoreState)?.form?.isLoading == true
        case "Lab Result": return (state as? LabResultEditStoreState)?.form?.isLoading == true
        case "Imaging": return (state as? ImagingEditStoreState)?.form?.isLoading == true
        case "Reproduction": return (state as? ReproductionEventEditStoreState)?.form?.isLoading == true
        case "Ultrasound": return (state as? UltrasoundEditStoreState)?.form?.isLoading == true
        case "Gestation": return (state as? GestationEditStoreState)?.form?.isLoading == true
        case "Repro Medication": return (state as? ReproMedicationEditStoreState)?.form?.isLoading == true
        case "Embryo Transfer": return (state as? EmbryoTransferEditStoreState)?.form?.isLoading == true
        case "ICSI": return (state as? IcsiEditStoreState)?.form?.isLoading == true
        default: return false
        }
    }

    private static func fields(
        from state: AnyObject,
        displayType: String
    ) -> [RecordDetailNav.FieldRow]? {
        switch displayType {
        case "Consultation":
            return (state as? ConsultationEditStoreState).flatMap { $0.form }.map(RecordDetailContent.consultationFields)
        case "Weight":
            return (state as? WeightEditStoreState).flatMap { $0.form }.map(RecordDetailContent.weightFields)
        case "Vaccination":
            return (state as? VaccinationEditStoreState).flatMap { $0.form }.map(RecordDetailContent.vaccinationFields)
        case "Deworming":
            return (state as? DewormingEditStoreState).flatMap { $0.form }.map(RecordDetailContent.dewormingFields)
        case "Dentistry":
            return (state as? DentistryEditStoreState).flatMap { $0.form }.map(RecordDetailContent.dentistryFields)
        case "Farrier":
            return (state as? FarrierVisitEditStoreState).flatMap { $0.form }.map(RecordDetailContent.farrierFields)
        case "Lameness":
            return (state as? LamenessEditStoreState).flatMap { $0.form }.map(RecordDetailContent.lamenessFields)
        case "Surgery":
            return (state as? SurgeryEditStoreState).flatMap { $0.form }.map(RecordDetailContent.surgeryFields)
        case "Medication":
            return (state as? MedicationEditStoreState).flatMap { $0.form }.map(RecordDetailContent.medicationFields)
        case "Controlled Substance":
            return (state as? SubstanceEditStoreState).flatMap { $0.form }.map(RecordDetailContent.substanceFields)
        case "Lab Result":
            return (state as? LabResultEditStoreState).flatMap { $0.form }.map(RecordDetailContent.labResultFields)
        case "Imaging":
            return (state as? ImagingEditStoreState).flatMap { $0.form }.map(RecordDetailContent.imagingFields)
        case "Reproduction":
            return (state as? ReproductionEventEditStoreState).flatMap { $0.form }.map(RecordDetailContent.reproductionFields)
        case "Ultrasound":
            return (state as? UltrasoundEditStoreState).flatMap { $0.form }.map(RecordDetailContent.ultrasoundFields)
        case "Gestation":
            return (state as? GestationEditStoreState).flatMap { $0.form }.map(RecordDetailContent.gestationFields)
        case "Repro Medication":
            return (state as? ReproMedicationEditStoreState).flatMap { $0.form }.map(RecordDetailContent.reproMedicationFields)
        case "Embryo Transfer":
            return (state as? EmbryoTransferEditStoreState).flatMap { $0.form }.map(RecordDetailContent.embryoTransferFields)
        case "ICSI":
            return (state as? IcsiEditStoreState).flatMap { $0.form }.map(RecordDetailContent.icsiFields)
        default:
            return nil
        }
    }

    private static func storeBox(
        for displayType: String,
        patientId: Int64,
        recordId: Int64
    ) -> StoreBox? {
        switch displayType {
        case "Consultation":
            let store = RecordStores.consultationEditStore(patientId: patientId, consultationId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Weight":
            let store = RecordStores.weightEditStore(patientId: patientId, weightId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Vaccination":
            let store = RecordStores.vaccinationEditStore(patientId: patientId, vaccinationId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Deworming":
            let store = RecordStores.dewormingEditStore(patientId: patientId, dewormingId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Dentistry":
            let store = RecordStores.dentistryEditStore(patientId: patientId, dentistryId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Farrier":
            let store = RecordStores.farrierVisitEditStore(patientId: patientId, farrierVisitId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Lameness":
            let store = RecordStores.lamenessEditStore(patientId: patientId, lamenessId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Surgery":
            let store = RecordStores.surgeryEditStore(patientId: patientId, surgeryId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Medication":
            let store = RecordStores.medicationEditStore(patientId: patientId, medicationId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Controlled Substance":
            let store = RecordStores.substanceEditStore(patientId: patientId, substanceId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Lab Result":
            let store = RecordStores.labResultEditStore(patientId: patientId, labResultId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Imaging":
            let store = RecordStores.imagingEditStore(patientId: patientId, imagingId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Reproduction":
            let store = RecordStores.reproductionEventEditStore(patientId: patientId, reproductionEventId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Ultrasound":
            let store = RecordStores.ultrasoundEditStore(patientId: patientId, ultrasoundId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Gestation":
            let store = RecordStores.gestationEditStore(patientId: patientId, gestationId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Repro Medication":
            let store = RecordStores.reproMedicationEditStore(patientId: patientId, reproMedicationId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "Embryo Transfer":
            let store = RecordStores.embryoTransferEditStore(patientId: patientId, embryoTransferId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        case "ICSI":
            let store = RecordStores.icsiEditStore(patientId: patientId, icsiId: recordId)
            return StoreBox(
                initial: store.state.current,
                subscribe: { callback in store.state.subscribe(onEach: { callback($0) }) }
            )
        default:
            return nil
        }
    }
}
