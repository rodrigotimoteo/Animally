import SwiftUI
import Shared

struct TimelineView: View {
    @StateObject private var viewModel = TimelineViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.state.isLoading && viewModel.state.groups.isEmpty {
                    loadingView
                } else if viewModel.state.groups.isEmpty {
                    emptyView
                } else {
                    timelineList
                }
            }
            .navigationTitle("Timeline")
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
        }
    }

    private var timelineList: some View {
        List {
            ForEach(viewModel.state.groups, id: \.date) { group in
                Section {
                    ForEach(group.entries, id: \.recordId) { entry in
                        NavigationLink(value: Route.patientDetail(entry.patientId)) {
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
