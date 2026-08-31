import Foundation
import Shared

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var themeMode: ThemeMode
    @Published var accentColor: AccentColor
    @Published var patients: [Patient_]
    @Published var selectedPatientId: Int64?
    @Published var restoreJson: String
    @Published var backupStatus: String?
    @Published var restoreStatus: String?
    @Published var pdfStatus: String?
    // Danger zone: irreversible database wipe.
    @Published var isWipingData = false
    @Published var dataWiped = false
    @Published var wipeStatus: String?
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
        accentColor = store.accentColor
        patients = store.patients
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

    /// Erases every table and resets the search index. Call only after the
    /// user confirmed in the confirmation dialog — irreversible.
    func wipeAllData() {
        store.wipeAllData()
        isWipingData = store.isWipingData
        dataWiped = store.dataWiped
        wipeStatus = store.wipeStatus
    }

    func setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode: mode)
    }

    func setAccentColor(_ accent: AccentColor) {
        store.setAccentColor(accent: accent)
        accentColor = store.accentColor
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
        let all = store.cloudProviderPresets
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
        do {
            try await store.fetchCloudModels()
            // Kotlin clears the status on a new attempt and sets it on failure,
            // so nil here means success.
            cloudModelsStatus = store.cloudModelsStatus
        } catch {
            // The bridged suspend call can still throw across the ObjC boundary
            // (e.g. bridge/threading failures); surface it instead of swallowing.
            cloudModelsStatus = "Could not fetch models: \(error.localizedDescription)"
        }
        cloudModelChoices = store.cloudModelChoices
        isFetchingCloudModels = false
    }

    deinit {
        cancellable?.cancel()
    }
}
