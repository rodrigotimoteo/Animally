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
                errorBanner(message: errorMessage)
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
                recordId: key.recordId,
                onEdit: {
                    if let editRoute = RecordEditRoute(
                        displayType: key.displayType,
                        patientId: key.patientId,
                        recordId: key.recordId
                    ) {
                        path.append(editRoute)
                    }
                }
            )
        }
        }

    private var timelineList: some View {
        List {
            ForEach(viewModel.state.groups, id: \.date) { group in
                Section {
                    ForEach(group.entries, id: \.recordId) { entry in
                        Button {
                            // Open the read-only record detail with the patient
                            // page underneath so Back returns to it.
                            path.append(Route.patientDetail(entry.patientId))
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
                    Text(formatDate(group.date))
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

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private func formatDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        return "\(date.year)-\(String(format: "%02d", date.monthNumber))-\(String(format: "%02d", date.dayOfMonth))"
    }
}

struct TimelineEntryRow: View {
    let entry: TimelineEntry
    let showPatientName: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconForRecordType(entry.recordType))
                .font(.title2)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 44, height: 44)
                .background(Theme.forestGreen.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(entry.title)
                    .font(.headline)
                    .foregroundStyle(Theme.textPrimary)

                Text(entry.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(2)

                if showPatientName {
                    Text(entry.patientName)
                        .font(.caption)
                        .foregroundStyle(Theme.textTertiary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Timeline entry: \(entry.title)")
    }

    private func iconForRecordType(_ type: String) -> String {
        switch type {
        case "Vaccination":
            return "syringe.fill"
        case "Deworming":
            return "pills.fill"
        case "Consultation":
            return "stethoscope"
        case "Weight":
            return "scalemass.fill"
        case "Reproduction":
            return "heart.fill"
        case "Farrier":
            return "figure.walk"
        case "Dentistry":
            return "mouth.fill"
        default:
            return "doc.text.fill"
        }
    }
}
