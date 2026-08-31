import SwiftUI
import Shared

struct TimelineView: View {
    @StateObject private var patientListViewModel = PatientListViewModel()
    @State private var selectedPatientId: Int64?
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            TimelineContent(patientId: selectedPatientId, path: $path)
                .id(selectedPatientId)
                .navigationTitle("Timeline")
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        NavigationLink(value: InsightsNavKey(patientId: nil, patientName: nil)) {
                            Image(systemName: "chart.bar.doc.horizontal")
                                .accessibilityLabel("Insights")
                        }
                        .accessibilityIdentifier("timeline_insights_button")
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        patientFilterMenu
                    }
                }
                .onAppear {
                    patientListViewModel.load()
                }
        }
    }

    private var patientFilterMenu: some View {
        Menu {
            Picker("Animal", selection: $selectedPatientId) {
                Text("All animals").tag(Int64?.none)
                ForEach(patientListViewModel.state.patients, id: \.id) { patient in
                    Text(patient.name).tag(Int64?.some(patient.id))
                }
            }
        } label: {
            Image(systemName: selectedPatientId == nil
                ? "line.3.horizontal.decrease.circle"
                : "line.3.horizontal.decrease.circle.fill"
            )
            .foregroundStyle(selectedPatientId == nil ? Theme.textSecondary : Theme.forestGreen)
            .accessibilityLabel("Filter timeline by animal")
        }
    }
}

/// The timeline feed itself. Re-created whenever the selected patient changes,
/// which builds a fresh TimelineViewModel bound to that patient's store.
private struct TimelineContent: View {
    @StateObject private var viewModel: TimelineViewModel
    @Binding var path: NavigationPath

    init(
        patientId: Int64?,
        path: Binding<NavigationPath>,
    ) {
        _viewModel = StateObject(wrappedValue: TimelineViewModel(patientId: patientId))
        _path = path
    }

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.groups.isEmpty {
                loadingView
            } else if viewModel.state.groups.isEmpty {
                emptyView
            } else {
                timelineList
            }
        }
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.state.errorMessage {
                InlineErrorBanner(message: errorMessage, onDismiss: { viewModel.dismissError() })
            }
        }
        .onAppear {
            viewModel.load()
        }
        .refreshable {
            viewModel.load()
        }
        .navigationDestination(for: Route.self) { route in
            switch route {
            case .patientDetail(let id):
                PatientDetailView(patientId: id)
            case .patientEdit(let id):
                PatientEditView(patientId: id)
            case .ownerDetail(let id):
                OwnerDetailView(ownerId: id)
            case .ownerEdit(let id):
                OwnerEditView(ownerId: id)
            }
        }
        .navigationDestination(for: RecordEditRoute.self) { route in
            recordEditDestination(route)
        }
        .navigationDestination(for: RecordDetailKey.self) { key in
            RecordDetailView(
                displayType: key.displayType,
                patientId: key.patientId,
                recordId: key.recordId
            )
        }
        .navigationDestination(for: InsightsNavKey.self) { key in
            InsightsView(patientId: key.patientId, patientName: key.patientName)
        }
    }

    private var timelineList: some View {
        List {
            ForEach(viewModel.state.groups, id: \.date) { group in
                Section {
                    ForEach(group.entries, id: \.recordId) { entry in
                        Button {
                            path.append(RecordDetailKey(
                                displayType: entry.recordType,
                                patientId: entry.patientId,
                                recordId: entry.recordId
                            ))
                        } label: {
                            TimelineEntryRow(entry: entry, showPatientName: viewModel.state.patientId == nil)
                        }
                        .buttonStyle(.plain)
                    }
                } header: {
                    Text(group.date.displayString)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                        .textCase(nil)
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading timeline…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyView: some View {
        VStack(spacing: 20) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.6))
            Text("No events yet")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Timeline events will appear here")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct TimelineEntryRow: View {
    let entry: TimelineEntry
    let showPatientName: Bool

    var body: some View {
        RecordRowView(
            icon: RecordTypeIcon.systemName(for: entry.recordType),
            iconTint: Theme.forestGreen,
            title: entry.title,
            subtitle: subtitleText,
            date: nil,
            badgeSize: 44,
            badgeCorner: 22
        )
        .accessibilityLabel("Timeline entry: \(entry.title), \(entry.subtitle)\(showPatientName ? ", \(entry.patientName)" : "")")
    }

    private var subtitleText: String {
        if showPatientName { "\(entry.subtitle) · \(entry.patientName)" } else { entry.subtitle }
    }
}
