import Foundation
import Shared

/// Manages the Preventive tab: vaccination, deworming, dentistry, farrierVisit.
@MainActor
final class PreventiveTabViewModel: ObservableObject {
    @Published var vaccinations: [Vaccination_] = []
    @Published var dewormings: [Deworming_] = []
    @Published var dentistryRecords: [Dentistry_] = []
    @Published var farrierVisits: [FarrierVisit_] = []

    @Published var isLoading: Bool = true

    private var cancellables: [NativeCancellable] = []

    private let vaccinationStore: VaccinationListStore
    private let dewormingStore: DewormingListStore
    private let dentistryStore: DentistryListStore
    private let farrierStore: FarrierVisitListStore

    init(patientId: Int64) {
        vaccinationStore = IosRecordStores.shared.vaccinationListStore(patientId: patientId)
        dewormingStore = IosRecordStores.shared.dewormingListStore(patientId: patientId)
        dentistryStore = IosRecordStores.shared.dentistryListStore(patientId: patientId)
        farrierStore = IosRecordStores.shared.farrierVisitListStore(patientId: patientId)

        cancellables.append(vaccinationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.vaccinations = state.vaccinations }
        }))
        cancellables.append(dewormingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.dewormings = state.records }
        }))
        cancellables.append(dentistryStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.dentistryRecords = state.records }
        }))
        cancellables.append(farrierStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.farrierVisits = state.records }
        }))

        vaccinationStore.load()
        dewormingStore.load()
        dentistryStore.load()
        farrierStore.load()

        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(100))
            self.isLoading = false
        }
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteVaccination(_ recordId: Int64) {
        vaccinationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteDeworming(_ recordId: Int64) {
        dewormingStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteDentistry(_ recordId: Int64) {
        dentistryStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteFarrierVisit(_ recordId: Int64) {
        farrierStore.delete(recordId: recordId)
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
