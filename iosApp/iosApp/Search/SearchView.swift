import SwiftUI
import Shared

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    @State private var path = NavigationPath()
    /// Keep text-entry state local to SwiftUI. A round-trip through the
    /// Kotlin StateFlow on every keystroke can lag behind UIKit and drop the
    /// tail of a fast query; the view model remains the search source of truth
    /// after each complete local edit.
    @State private var searchText = ""

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if searchText.isEmpty && viewModel.state.results.isEmpty {
                    emptySearchView
                } else if viewModel.state.results.isEmpty && !searchText.isEmpty {
                    noResultsView
                } else {
                    resultsList
                }
            }
            .searchable(text: $searchText, prompt: "Search patients, records, and more")
            .navigationTitle("Search")
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.state.errorMessage {
                    InlineErrorBanner(message: errorMessage, onDismiss: { viewModel.dismissError() })
                }
            }
            .safeAreaInset(edge: .top) {
                if !searchText.isEmpty {
                    filterChips
                }
            }
            .onAppear {
                searchText = viewModel.state.query
            }
            .onChange(of: searchText) { _, newValue in
                viewModel.setQuery(query: newValue)
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
        }
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(viewModel.state.recordTypeOptions), id: \.first) { option in
                    if let rawRecordType = option.first, let rawLabel = option.second {
                        let recordType = String(rawRecordType)
                        let label = String(rawLabel)
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
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Theme.surfaceElevated.opacity(0.5))
    }

    private var resultsList: some View {
        List {
            ForEach(viewModel.state.results, id: \.self) { result in
                Group {
                    if result.recordType == "OWNER" {
                        NavigationLink(value: Route.ownerDetail(result.patientId)) {
                            SearchResultRow(result: result, showsDisclosureIndicator: false)
                        }
                        .buttonStyle(.plain)
                    } else if result.recordType == "PATIENT" {
                        NavigationLink(value: Route.patientDetail(result.patientId)) {
                            SearchResultRow(result: result, showsDisclosureIndicator: false)
                        }
                        .buttonStyle(.plain)
                    } else {
                        Button {
                            path.append(Route.patientDetail(result.patientId))
                            path.append(RecordDetailKey(
                                displayType: result.recordType,
                                patientId: result.patientId,
                                recordId: result.recordId
                            ))
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

}

struct SearchResultRow: View {
    let result: SearchResult
    var showsDisclosureIndicator = true

    var body: some View {
        RecordRowView(
            icon: RecordTypeIcon.systemName(for: result.recordType),
            iconTint: Theme.forestGreen,
            title: result.patientName,
            subtitle: result.snippet,
            date: result.date?.displayString,
            showsDisclosure: showsDisclosureIndicator,
            badgeSize: 44,
            badgeCorner: 22
        )
        .accessibilityLabel("Result for \(result.patientName), \(result.recordType): \(result.snippet)")
    }
}
