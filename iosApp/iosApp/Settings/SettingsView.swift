import SwiftUI
import Shared

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var theme: ThemeViewModel

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
                    dataSection
                    pdfSection
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
