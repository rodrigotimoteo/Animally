import Foundation
import Shared

/// Manages the Reproduction tab: reproduction events, ultrasounds, gestations, repro medications.
@MainActor
final class ReproductionTabViewModel: ObservableObject {
    @Published var reproductionEvents: [ReproductionEvent] = []
    @Published var ultrasounds: [Ultrasound_] = []
    @Published var gestations: [Gestation_] = []
    @Published var reproMedications: [ReproMedication_] = []
    @Published var embryoTransfers: [EmbryoTransfer_] = []
    @Published var icsiRecords: [Icsi_] = []

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()

    private var cancellables: [NativeCancellable] = []

    private let reproStore: ReproductionListStore
    private let ultrasoundStore: UltrasoundListStore
    private let gestationStore: GestationListStore
    private let reproMedStore: ReproMedicationListStore
    private let embryoTransferStore: EmbryoTransferListStore
    private let icsiStore: IcsiListStore

    init(patientId: Int64) {
        reproStore = IosReproAndDiagnosticsStores.shared.reproductionListStore(patientId: patientId)
        ultrasoundStore = IosReproAndDiagnosticsStores.shared.ultrasoundListStore(patientId: patientId)
        gestationStore = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        reproMedStore = IosReproAndDiagnosticsStores.shared.reproMedicationListStore(patientId: patientId)
        embryoTransferStore = IosRecordStores.shared.embryoTransferListStore(patientId: patientId)
        icsiStore = IosRecordStores.shared.icsiListStore(patientId: patientId)

        cancellables.append(reproStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.reproductionEvents = state.records }
            self?.markFirstEmission("reproduction")
        }))
        cancellables.append(ultrasoundStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.ultrasounds = state.records }
            self?.markFirstEmission("ultrasound")
        }))
        cancellables.append(gestationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.gestations = state.records }
            self?.markFirstEmission("gestation")
        }))
        cancellables.append(reproMedStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.reproMedications = state.records }
            self?.markFirstEmission("reproMedications")
        }))
        cancellables.append(embryoTransferStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.embryoTransfers = state.records }
            self?.markFirstEmission("embryoTransfers")
        }))
        cancellables.append(icsiStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.icsiRecords = state.records }
            self?.markFirstEmission("icsi")
        }))

        reproStore.load()
        ultrasoundStore.load()
        gestationStore.load()
        reproMedStore.load()
        embryoTransferStore.load()
        icsiStore.load()

    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteReproductionEvent(_ recordId: Int64) {
        reproStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteUltrasound(_ recordId: Int64) {
        ultrasoundStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteGestation(_ recordId: Int64) {
        gestationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteReproMedication(_ recordId: Int64) {
        reproMedStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteEmbryoTransfer(_ recordId: Int64) {
        embryoTransferStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteIcsi(_ recordId: Int64) {
        icsiStore.delete(recordId: recordId)
    }

    private func markFirstEmission(_ key: String) {
        receivedStoreKeys.insert(key)
        isLoading = receivedStoreKeys.count < 6
    }

    /// Reloads every store this tab owns; stores re-query Kotlin and republish.
    func reload() {
        reproStore.load()
        ultrasoundStore.load()
        gestationStore.load()
        reproMedStore.load()
        embryoTransferStore.load()
        icsiStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
