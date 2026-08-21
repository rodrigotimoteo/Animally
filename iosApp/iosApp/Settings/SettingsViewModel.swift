import Foundation
import Shared

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var themeMode: ThemeMode
    @Published var patients: [Patient_]
    @Published var selectedPatientId: Int64?
    @Published var restoreJson: String
    @Published var backupStatus: String?
    @Published var restoreStatus: String?
    @Published var pdfStatus: String?

    private let store: SettingsStore
    private var cancellable: NativeCancellable?

    init() {
        store = IosSettingsStores.shared.settingsStore()
        themeMode = store.state.current
        patients = store.patients as? [Patient_] ?? []
        selectedPatientId = store.selectedPatientId?.int64Value
        restoreJson = store.restoreJson
        backupStatus = store.backupStatus
        restoreStatus = store.restoreStatus
        pdfStatus = store.pdfStatus

        cancellable = store.state.subscribe(onEach: { [weak self] newMode in
            Task { @MainActor in
                self?.themeMode = newMode
            }
        })
    }

    func exportCsv() {
        store.exportCsv()
    }

    func exportBackup() {
        store.exportBackup()
        backupStatus = store.backupStatus
    }

    func restoreBackup() {
        store.restoreJson = restoreJson
        store.restoreBackup()
        restoreStatus = store.restoreStatus
    }

    func selectPatient(patientId: Int64) {
        store.selectPatient(patientId: patientId)
        selectedPatientId = patientId
    }

    func exportPdf() {
        store.exportPdf()
        pdfStatus = store.pdfStatus
    }

    func setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode: mode)
    }

    deinit {
        cancellable?.cancel()
    }
}
