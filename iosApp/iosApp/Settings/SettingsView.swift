import SwiftUI
import Shared

private let maximumRestoreInputUTF16Units = 4_194_304

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
            .sheet(isPresented: $showModelPicker) {
                CloudModelPickerSheet(
                    models: viewModel.cloudModelChoices,
                    accentColor: selectedAccentColor,
                    onSelect: { model in
                        viewModel.setCloudModel(model)
                        showModelPicker = false
                    }
                )
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .tint(selectedAccentColor)
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

    private var accentColors: [AccentColor] {
        let values = AccentColor.values()
        var result: [AccentColor] = []
        for i in 0..<Int(values.size) {
            if let accent = values.get(index: Int32(i)) {
                result.append(accent)
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
            .accessibilityIdentifier("settings_theme_picker")
            .accessibilityValue(viewModel.themeMode.label)
            .onChange(of: viewModel.themeMode) { _, newValue in
                viewModel.setThemeMode(mode: newValue)
                // Keep the already-presented sheet in sync even if the
                // NSUserDefaults notification is delivered asynchronously.
                theme.reloadFromPreferences()
            }

            LazyVGrid(columns: [GridItem(.adaptive(minimum: 82), spacing: 12)], spacing: 12) {
                ForEach(accentColors, id: \.self) { accent in
                    Button {
                        viewModel.setAccentColor(accent)
                        // The settings VM writes through Kotlin, while the
                        // root theme VM owns the SwiftUI environment. Refresh
                        // it immediately so the tab bar, sheets, and visible
                        // settings controls all use the new accent.
                        theme.reloadFromPreferences()
                    } label: {
                        VStack(spacing: 6) {
                            ZStack {
                                Circle()
                                    .fill(Theme.color(for: accent))
                                    .frame(width: 34, height: 34)
                                if viewModel.accentColor == accent {
                                    Image(systemName: "checkmark")
                                        .font(.caption.weight(.bold))
                                        .foregroundStyle(.white)
                                }
                            }
                            Text(accent.label)
                                .font(.caption.weight(.medium))
                                .foregroundStyle(Theme.textPrimary)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Accent \(accent.label)")
                    .accessibilityValue(viewModel.accentColor == accent ? "Selected" : "")
                    .accessibilityIdentifier("settings_accent_\(accent.id)")
                }
            }
            .padding(.vertical, 4)
        } header: {
            sectionHeader("Appearance")
        } footer: {
            Text("Choose a color that feels right. It updates the app immediately and is saved for next time.")
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
                Menu {
                    ForEach(viewModel.cloudProviderPresets, id: \.self) { preset in
                        Button {
                            viewModel.setCloudProviderPreset(preset)
                        } label: {
                            if preset == viewModel.cloudProviderPreset {
                                Label(preset.displayName, systemImage: "checkmark")
                            } else {
                                Text(preset.displayName)
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text("Provider")
                        Spacer()
                        Text(viewModel.cloudProviderPreset.displayName)
                            .foregroundStyle(selectedAccentColor)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(selectedAccentColor)
                    }
                }
                .tint(selectedAccentColor)
                .accessibilityLabel("Provider")
                .accessibilityValue(viewModel.cloudProviderPreset.displayName)
                .accessibilityIdentifier("settings_cloud_provider")

                SecureField("API Key", text: Binding(
                    get: { viewModel.cloudApiKey },
                    set: { viewModel.setCloudApiKey($0) }
                ))
                .accessibilityIdentifier("settings_cloud_api_key")

                HStack {
                    if showsModelPicker {
                        // Fetched models exist: the field becomes a picker row;
                        // tapping opens the searchable sheet.
                        Button {
                            showModelPicker = true
                        } label: {
                            HStack {
                                Text(viewModel.cloudModel.isEmpty ? "Choose a model" : viewModel.cloudModel)
                                    .foregroundStyle(viewModel.cloudModel.isEmpty ? Theme.textSecondary : Theme.textPrimary)
                                Spacer()
                                Image(systemName: "chevron.up.chevron.down")
                                    .font(.caption.weight(.medium))
                                    .foregroundStyle(Theme.textSecondary)
                            }
                        }
                        .accessibilityIdentifier("settings_cloud_model")
                    } else {
                        TextField("Model", text: Binding(
                            get: { viewModel.cloudModel },
                            set: { viewModel.setCloudModel($0) }
                        ))
                        .accessibilityIdentifier("settings_cloud_model")
                    }

                    Button("Fetch models") {
                        Task { await viewModel.fetchCloudModels() }
                    }
                    .accessibilityIdentifier("settings_cloud_fetch_models")
                    .disabled(viewModel.isFetchingCloudModels)

                    if viewModel.isFetchingCloudModels {
                        ProgressView()
                    }
                }

                if let status = viewModel.cloudModelsStatus {
                    Text(status)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                        .accessibilityIdentifier("settings_cloud_models_status")
                }

                DisclosureGroup {
                    TextField("Endpoint URL", text: Binding(
                        get: { viewModel.cloudBaseUrl },
                        set: { viewModel.setCloudBaseUrl($0) }
                    ))
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .accessibilityIdentifier("settings_cloud_base_url")
                } label: {
                    // Put the test/accessibility identity on the visible
                    // disclosure label. Applying it to DisclosureGroup itself
                    // can hide the child TextField from XCTest after expand.
                    Text("Advanced")
                        .accessibilityIdentifier("settings_cloud_advanced")
                }
            }
        } header: {
            sectionHeader("Cloud AI")
        } footer: {
            Text("When enabled, the assistant falls back to a cloud model if on-device AI is unavailable. The key is stored in the Keychain.")
        }
    }

    /// Manual typing only under the Custom preset; known providers pick from
    /// fetched models once discovery has results.
    private var showsModelPicker: Bool {
        viewModel.cloudProviderPreset != CloudLlmProviderPreset.custom &&
            !viewModel.cloudModelChoices.isEmpty
    }

    private var dataSection: some View {
        Section {
            Button {
                viewModel.exportCsv()
            } label: {
                Label("Export CSV", systemImage: "tablecells")
            }
            .disabled(viewModel.isExportingCsv)

            if let status = viewModel.csvStatus {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            }

            Button {
                viewModel.exportBackup()
            } label: {
                Label("Export Backup", systemImage: "square.and.arrow.up")
            }
            .disabled(viewModel.isExportingBackup)

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

            TextEditor(
                text: Binding(
                    get: { viewModel.restoreJson },
                    set: { value in
                        if value.utf16.count <= maximumRestoreInputUTF16Units {
                            viewModel.restoreJson = value
                        }
                    }
                )
            )
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
                    .background(selectedAccentColor)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(viewModel.restoreJson.isEmpty || viewModel.isRestoringBackup)

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
                        .foregroundStyle(selectedAccentColor)
                }
                .disabled(
                    viewModel.selectedPatientId == nil ||
                    viewModel.selectedPatientId == 0 ||
                    viewModel.isExportingPdf
                )

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
            .foregroundStyle(selectedAccentColor)
            .textCase(nil)
    }

    private var selectedAccentColor: Color {
        Theme.color(for: viewModel.accentColor)
    }
}

enum SettingsRoute: Hashable {
    case restoreBackup
}

/// Searchable sheet over the last-fetched model list; tapping a row selects it.
private struct CloudModelPickerSheet: View {
    let models: [String]
    let accentColor: Color
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
                            .accessibilityIdentifier("cloud_model_row")
                    }
                }
            }
            .navigationTitle("Choose a model")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $filter, prompt: "Filter models")
            .tint(accentColor)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
