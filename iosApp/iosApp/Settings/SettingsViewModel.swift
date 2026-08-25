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
    // Cloud AI settings (API key round-trips through secure storage only).
    @Published var cloudAiEnabled: Bool
    @Published var cloudApiKey: String
    @Published var cloudModel: String
    @Published var cloudBaseUrl: String
    // Provider presets + model discovery.
    @Published var cloudProviderPreset: CloudLlmProviderPreset
    @Published var cloudModelChoices: [String] = []
    @Published var cloudModelsStatus: String?
    @Published var isFetchingCloudModels = false

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
        cloudAiEnabled = store.cloudAiEnabled
        cloudApiKey = store.cloudApiKey
        cloudModel = store.cloudModel
        cloudBaseUrl = store.cloudBaseUrl
        cloudProviderPreset = store.cloudProviderPreset

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

    func setCloudAiEnabled(_ enabled: Bool) {
        store.setCloudAiEnabled(enabled: enabled)
        cloudAiEnabled = store.cloudAiEnabled
    }

    func setCloudApiKey(_ key: String) {
        store.setCloudApiKey(key: key)
        cloudApiKey = key
    }

    func setCloudModel(_ model: String) {
        store.setCloudModel(model: model)
        cloudModel = model
    }

    func setCloudBaseUrl(_ url: String) {
        store.setCloudBaseUrl(url: url)
        cloudBaseUrl = url
    }

    /// Selectable provider presets, in display order. Local runtimes
    /// (Ollama/LM Studio) point at localhost and are hidden on iOS.
    var cloudProviderPresets: [CloudLlmProviderPreset] {
        let all = (store.cloudProviderPresets as? [CloudLlmProviderPreset]) ?? []
        return all.filter { $0.visibleOnMobile }
    }

    /// Selects a provider preset; non-custom presets also fill the endpoint URL.
    func setCloudProviderPreset(_ preset: CloudLlmProviderPreset) {
        store.setCloudProviderPreset(preset: preset)
        cloudProviderPreset = store.cloudProviderPreset
        cloudBaseUrl = store.cloudBaseUrl
    }

    /// Fetches the model list from the configured endpoint, then reads back the
    /// cached results. Local presets (Ollama / LM Studio) work keyless.
    func fetchCloudModels() async {
        isFetchingCloudModels = true
        // The Kotlin side never throws (all failures land in cloudModelsStatus);
        // the bridged signature still declares `throws`, so swallow here.
        try? await store.fetchCloudModels()
        cloudModelChoices = (store.cloudModelChoices as? [String]) ?? []
        cloudModelsStatus = store.cloudModelsStatus
        isFetchingCloudModels = false
    }

    deinit {
        cancellable?.cancel()
    }
}
