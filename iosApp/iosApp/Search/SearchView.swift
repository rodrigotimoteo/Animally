import SwiftUI
import Shared

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if viewModel.state.query.isEmpty && viewModel.state.results.isEmpty {
                    emptySearchView
                } else if viewModel.state.results.isEmpty && !viewModel.state.query.isEmpty {
                    noResultsView
                } else {
                    resultsList
                }
            }
            .searchable(text: Binding(
                get: { viewModel.state.query },
                set: { viewModel.setQuery(query: $0) }
            ), prompt: "Search patients, records, and more")
            .navigationTitle("Search")
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.state.errorMessage {
                    errorBanner(message: errorMessage)
                }
            }
            .safeAreaInset(edge: .top) {
                if !viewModel.state.query.isEmpty {
                    filterChips
                }
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
        }
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(viewModel.state.recordTypeOptions), id: \.first) { option in
                    let recordType = String(option.first!)
                    let label = String(option.second!)
                    let isSelected = viewModel.state.recordTypes.contains(recordType)

                    Button {
                        viewModel.toggleRecordType(recordType: recordType)
                    } label: {
                        Text(label)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(isSelected ? Theme.forestGreen : Theme.surfaceElevated)
                            .foregroundStyle(isSelected ? .white : Theme.textPrimary)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Theme.surfaceElevated.opacity(0.5))
    }

    private var resultsList: some View {
        List {
            ForEach(Array(viewModel.state.results.enumerated()), id: \.offset) { _, result in
                Group {
                    if result.recordType == "OWNER" {
                        // Owner hits navigate to the owner; patientId mirrors
                        // the owner id for these rows.
                        NavigationLink(value: Route.ownerDetail(result.patientId)) {
                            SearchResultRow(result: result)
                        }
                        .buttonStyle(.plain)
                    } else if result.recordType == "PATIENT" {
                        NavigationLink(value: Route.patientDetail(result.patientId)) {
                            SearchResultRow(result: result)
                        }
                        .buttonStyle(.plain)
                    } else {
                        // Record hits deep-link straight into the record editor
                        // with the patient page underneath so Back returns to it.
                        Button {
                            path.append(Route.patientDetail(result.patientId))
                            if let editRoute = RecordEditRoute(
                                displayType: result.recordType,
                                patientId: result.patientId,
                                recordId: result.recordId
                            ) {
                                path.append(editRoute)
                            }
                        } label: {
                            SearchResultRow(result: result)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var emptySearchView: some View {
        VStack(spacing: 20) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.6))
            Text("Search patients and records")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Find patients, consultations, medications, and more")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    private var noResultsView: some View {
        VStack(spacing: 20) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 64))
                .foregroundStyle(Theme.textTertiary)
            Text("No results found")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Try adjusting your search or filters")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
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
}

struct SearchResultRow: View {
    let result: SearchResult

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconForRecordType(result.recordType))
                .font(.title2)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 44, height: 44)
                .background(Theme.forestGreen.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(result.patientName)
                    .font(.headline)
                    .foregroundStyle(Theme.textPrimary)

                Text(result.snippet)
                    .font(.subheadline)
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(2)

                if let date = result.date {
                    Text(formatDate(date))
                        .font(.caption)
                        .foregroundStyle(Theme.textTertiary)
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textTertiary)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Result for \(result.patientName)")
    }

    private func iconForRecordType(_ type: String) -> String {
        switch type.uppercased() {
        case "PATIENT":
            return "pawprint.fill"
        case "OWNER":
            return "person.fill"
        case "CONSULTATION":
            return "stethoscope"
        case "MEDICATION":
            return "pills.fill"
        default:
            return "doc.text.fill"
        }
    }

    private func formatDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        return "\(date.year)-\(String(format: "%02d", date.monthNumber))-\(String(format: "%02d", date.dayOfMonth))"
    }
}
