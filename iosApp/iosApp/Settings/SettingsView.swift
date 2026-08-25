import SwiftUI
import Shared

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var theme: ThemeViewModel
    @State private var showModelPicker = false
    @State private var showWipeConfirmation = false

    var body: some View {
        // Presented as a sheet from the Patients toolbar gear; owns its navigation
        // stack so the title bar and the Restore Backup push work standalone.
        // Sheets capture color scheme at presentation time, so the scheme is
        // applied here too — otherwise theme changes only land after reopening.
        // navigationDestination sits on a non-lazy wrapper: registering it directly
        // on a List is ignored by SwiftUI.
        NavigationStack {
            Group {
                List {
                    themeSection
                    cloudAiSection
                    dataSection
                    pdfSection
                    dangerZoneSection
                }
                .listStyle(.insetGrouped)
            }
            .navigationTitle("Settings")
            .navigationDestination(for: SettingsRoute.self) { route in
                switch route {
                case .restoreBackup:
                    restoreView
                }
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .preferredColorScheme(theme.preferredColorScheme)
    }

    private var themeModes: [ThemeMode] {
        let values = ThemeMode.values()
        var result: [ThemeMode] = []
        for i in 0..<Int(values.size) {
            if let mode = values.get(index: Int32(i)) {
                result.append(mode)
            }
        }
        return result
    }

    private var themeSection: some View {
        Section {
            Picker("Theme", selection: $viewModel.themeMode) {
                ForEach(themeModes, id: \.self) { mode in
                    Text(mode.label).tag(mode)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: viewModel.themeMode) { _, newValue in
                viewModel.setThemeMode(mode: newValue)
            }
        } header: {
            sectionHeader("Appearance")
        }
    }

    /// Cloud AI: opt-in toggle, provider preset picker, API key (masked), model
    /// discovery ("Fetch models" + searchable picker), and a collapsed advanced
    /// endpoint field. Answers from the cloud model are badged in chat.
    private var cloudAiSection: some View {
        Section {
            Toggle("Cloud AI", isOn: Binding(
                get: { viewModel.cloudAiEnabled },
                set: { viewModel.setCloudAiEnabled($0) }
            ))

            if viewModel.cloudAiEnabled {
                Picker("Provider", selection: Binding(
                    get: { viewModel.cloudProviderPreset },
                    set: { viewModel.setCloudProviderPreset($0) }
                )) {
                    ForEach(viewModel.cloudProviderPresets, id: \.self) { preset in
                        Text(preset.displayName).tag(preset)
                    }
                }
                .accessibilityIdentifier("settings_cloud_provider")

                SecureField("API Key", text: Binding(
                    get: { viewModel.cloudApiKey },
                    set: { viewModel.setCloudApiKey($0) }
                ))
                .accessibilityIdentifier("settings_cloud_api_key")

                HStack {
                    TextField("Model", text: Binding(
                        get: { viewModel.cloudModel },
                        set: { viewModel.setCloudModel($0) }
                    ))
                    .accessibilityIdentifier("settings_cloud_model")

                    Button("Fetch models") {
                        Task { await viewModel.fetchCloudModels() }
                    }
                    .disabled(viewModel.isFetchingCloudModels)

                    if viewModel.isFetchingCloudModels {
                        ProgressView()
                    }
                }

                if let status = viewModel.cloudModelsStatus {
                    Text(status)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                }

                DisclosureGroup("Advanced") {
                    TextField("Endpoint URL", text: Binding(
                        get: { viewModel.cloudBaseUrl },
                        set: { viewModel.setCloudBaseUrl($0) }
                    ))
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .accessibilityIdentifier("settings_cloud_base_url")
                }
            }
        } header: {
            sectionHeader("Cloud AI")
        } footer: {
            Text("When enabled, the assistant falls back to a cloud model if on-device AI is unavailable. The key is stored in the Keychain.")
        }
        .sheet(isPresented: $showModelPicker) {
            CloudModelPickerSheet(
                models: viewModel.cloudModelChoices,
                onSelect: { model in
                    viewModel.setCloudModel(model)
                    showModelPicker = false
                }
            )
        }
    }

    private var dataSection: some View {
        Section {
            Button {
                viewModel.exportCsv()
            } label: {
                Label("Export CSV", systemImage: "tablecells")
            }

            Button {
                viewModel.exportBackup()
            } label: {
                Label("Export Backup", systemImage: "square.and.arrow.up")
            }

            if let status = viewModel.backupStatus {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            }

            NavigationLink(value: SettingsRoute.restoreBackup) {
                Label("Restore Backup", systemImage: "square.and.arrow.down")
            }

            if let status = viewModel.restoreStatus {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            }
        } header: {
            sectionHeader("Data")
        }
    }

    private var restoreView: some View {
        VStack(spacing: 16) {
            Text("Paste backup JSON below to restore your data.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)

            TextEditor(text: $viewModel.restoreJson)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 200)
                .padding(8)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            Button {
                viewModel.restoreBackup()
            } label: {
                Text("Restore")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Theme.forestGreen)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(viewModel.restoreJson.isEmpty)

            if let status = viewModel.restoreStatus {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Restore Backup")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var pdfSection: some View {
        Section {
            if viewModel.patients.isEmpty {
                Text("No patients available")
                    .foregroundStyle(Theme.textSecondary)
            } else {
                Picker("Patient", selection: Binding(
                    get: { viewModel.selectedPatientId ?? 0 },
                    set: { viewModel.selectPatient(patientId: $0) }
                )) {
                    Text("Select patient").tag(Int64(0))
                    ForEach(viewModel.patients, id: \.id) { patient in
                        Text(patient.name).tag(patient.id)
                    }
                }

                Button {
                    viewModel.exportPdf()
                } label: {
                    Label("Export PDF", systemImage: "doc.richtext")
                }
                .disabled(viewModel.selectedPatientId == nil || viewModel.selectedPatientId == 0)

                if let status = viewModel.pdfStatus {
                    Text(status)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                }
            }
        } header: {
            sectionHeader("PDF Export")
        }
    }

    /// Irreversible database wipe: red destructive action behind a
    /// confirmation dialog that states data cannot be recovered and
    /// recommends exporting a backup first.
    private var dangerZoneSection: some View {
        Section {
            Button {
                showWipeConfirmation = true
            } label: {
                if viewModel.isWipingData {
                    HStack {
                        ProgressView()
                        Text("Erasing…")
                    }
                } else {
                    Label("Erase All Data", systemImage: "trash")
                        .foregroundStyle(.red)
                }
            }
            .disabled(viewModel.isWipingData)

            if viewModel.dataWiped {
                Text("All data erased. Restart the app to start fresh.")
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            }

            if let status = viewModel.wipeStatus {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        } header: {
            sectionHeader("Danger Zone")
        } footer: {
            Text("Deletes every patient, owner and record. Cannot be undone.")
        }
        .confirmationDialog(
            "Erase All Data?",
            isPresented: $showWipeConfirmation,
            titleVisibility: .visible
        ) {
            Button("Erase Everything", role: .destructive) {
                viewModel.wipeAllData()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This permanently deletes every patient, owner and record in the app. "
                    + "This cannot be undone. Export a backup first if you may need this data."
            )
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
    }
}

enum SettingsRoute: Hashable {
    case restoreBackup
}

/// Searchable sheet over the last-fetched model list; tapping a row selects it.
private struct CloudModelPickerSheet: View {
    let models: [String]
    let onSelect: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var filter = ""

    private var filtered: [String] {
        filter.isEmpty ? models : models.filter { $0.localizedCaseInsensitiveContains(filter) }
    }

    var body: some View {
        NavigationStack {
            Group {
                if models.isEmpty {
                    ContentUnavailableView(
                        "No models fetched",
                        systemImage: "tray",
                        description: Text("Tap \"Fetch models\" first.")
                    )
                } else if filtered.isEmpty {
                    ContentUnavailableView.search(text: filter)
                } else {
                    List(filtered, id: \.self) { model in
                        Button(model) { onSelect(model) }
                    }
                }
            }
            .navigationTitle("Choose a model")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $filter, prompt: "Filter models")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
