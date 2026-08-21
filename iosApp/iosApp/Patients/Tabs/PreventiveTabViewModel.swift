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

    init(patientId: Int64) {
        let vaccinationStore = IosRecordStores.shared.vaccinationListStore(patientId: patientId)
        let dewormingStore = IosRecordStores.shared.dewormingListStore(patientId: patientId)
        let dentistryStore = IosRecordStores.shared.dentistryListStore(patientId: patientId)
        let farrierStore = IosRecordStores.shared.farrierVisitListStore(patientId: patientId)

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

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
