import SwiftUI
import Shared

struct PatientDetailView: View {
    @StateObject private var viewModel: PatientDetailViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab: DetailTab = .overview
    @State private var addRecordRoute: RecordEditRoute?
    @State private var selectedRecordKey: RecordDetailKey?
    // Only gestation needs a refresh token for day calc; other tabs react to store flows.
    @State private var gestationRefreshToken = 0

    enum DetailTab: String, CaseIterable, Identifiable {
        case overview = "Overview"
        case medical = "Medical"
        case preventive = "Care"
        case reproduction = "Repro"
        case diagnostics = "Files"

        var id: String { rawValue }

        var accessibilityName: String {
            switch self {
            case .overview: return "Overview"
            case .medical: return "Medical"
            case .preventive: return "Preventive"
            case .reproduction: return "Reproduction"
            case .diagnostics: return "Diagnostics & Files"
            }
        }
    }

    init(patientId: Int64) {
        _viewModel = StateObject(wrappedValue: PatientDetailViewModel(patientId: patientId))
    }

    var body: some View {
        let base = Group {
            if viewModel.state.isLoading && viewModel.state.patient == nil {
                loadingView
            } else if let patient = viewModel.state.patient {
                contentTabs(patient: patient)
            } else {
                notFoundView
            }
        }

        base
            .navigationTitle(viewModel.state.patient?.name ?? "Patient")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if let patientId = viewModel.state.patient?.id {
                        NavigationLink(value: InsightsNavKey(
                            patientId: patientId,
                            patientName: viewModel.state.patient?.name
                        )) {
                            Image(systemName: "chart.bar.doc.horizontal")
                                .accessibilityLabel("Insights for patient")
                        }
                        .accessibilityIdentifier("patient_detail_insights_button")
                        .disabled(viewModel.state.patient == nil)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink(value: Route.patientEdit(viewModel.state.patient?.id)) {
                        Image(systemName: "pencil")
                            .accessibilityLabel("Edit patient")
                    }
                    .disabled(viewModel.state.patient == nil)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if let patientId = viewModel.state.patient?.id {
                        AddRecordMenu(patientId: patientId) { route in
                            addRecordRoute = route
                        }
                    }
                }
            }
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.state.errorMessage {
                    InlineErrorBanner(message: errorMessage, onDismiss: { viewModel.dismissError() })
                }
            }
            .onAppear {
                viewModel.load()
                gestationRefreshToken += 1
            }
            .onChange(of: scenePhase) { _, phase in
                guard phase == .active else { return }
                viewModel.load()
                gestationRefreshToken += 1
            }
            .onChange(of: addRecordRoute) { oldValue, newValue in
                if oldValue != nil, newValue == nil {
                    viewModel.load()
                    gestationRefreshToken += 1
                }
            }
            .navigationDestination(item: $addRecordRoute) { route in
                recordEditDestination(route)
            }
            .navigationDestination(item: $selectedRecordKey) { key in
                RecordDetailView(displayType: key.displayType, patientId: key.patientId, recordId: key.recordId)
            }
    }

    private func makeOpenRecord(for patientId: Int64) -> (String, Int64) -> Void {
        { type, recordId in
            selectedRecordKey = RecordDetailKey(displayType: type, patientId: patientId, recordId: recordId)
        }
    }

    private func contentTabs(patient: Patient_) -> some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(DetailTab.allCases) { tab in
                        Button {
                            selectedTab = tab
                        } label: {
                            Text(tab.rawValue)
                                .font(.subheadline.weight(.semibold))
                                .padding(.horizontal, 14)
                                .frame(minHeight: 36)
                                .foregroundStyle(selectedTab == tab ? Color.white : Theme.textSecondary)
                                .background(
                                    selectedTab == tab
                                        ? Theme.forestGreen
                                        : Theme.surfaceElevated
                                )
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(tab.accessibilityName)
                        .accessibilityAddTraits(selectedTab == tab ? .isSelected : [])
                        .accessibilityIdentifier("patient_detail_tab_\(tab.rawValue.lowercased())")
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
            .accessibilityIdentifier("patient_detail_tabs")

            switch selectedTab {
            case .overview:
                OverviewTab(patient: patient, ownerName: viewModel.state.ownerName) {
                    selectedTab = .reproduction
                }
            case .medical:
                MedicalTabView(patientId: patient.id, onOpenRecord: makeOpenRecord(for: patient.id))
            case .preventive:
                PreventiveTabView(patientId: patient.id, onOpenRecord: makeOpenRecord(for: patient.id))
            case .reproduction:
                ReproductionTabView(patientId: patient.id, gestationRefreshToken: gestationRefreshToken, onOpenRecord: makeOpenRecord(for: patient.id))
            case .diagnostics:
                DiagnosticsTabView(patientId: patient.id, onOpenRecord: makeOpenRecord(for: patient.id))
            }
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading patient…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var notFoundView: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.amber)
            Text("Patient not found")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct OverviewTab: View {
    let patient: Patient_
    let ownerName: String?
    var onOpenReproduction: (() -> Void)? = nil

    @StateObject private var careModel: CareDuePanelModel
    @StateObject private var gestationModel: GestationPanelModel
    @State private var todayKotlin: Kotlinx_datetimeLocalDate
    @Environment(\.scenePhase) private var scenePhase

    init(
        patient: Patient_,
        ownerName: String?,
        onOpenReproduction: (() -> Void)? = nil
    ) {
        self.patient = patient
        self.ownerName = ownerName
        self.onOpenReproduction = onOpenReproduction
        _todayKotlin = State(initialValue: DateFormatters.todayLocalDate())
        _careModel = StateObject(wrappedValue: CareDuePanelModel(patientId: patient.id))
        _gestationModel = StateObject(wrappedValue: GestationPanelModel(patientId: patient.id))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let activeGestation = gestationModel.activeGestation {
                    pregnancyCard(activeGestation)
                }
                careDueSection
                patientHeader
                basicInfoSection
                if ownerName != nil {
                    ownerSection
                }
                identificationSection
            }
            .padding()
        }
        .onAppear { todayKotlin = DateFormatters.todayLocalDate() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                todayKotlin = DateFormatters.todayLocalDate()
            }
        }
    }

    private func pregnancyCard(_ gestation: Gestation_) -> some View {
        CompactGestationCard(
            gestation: gestation,
            gestationDay: gestationDay(for: gestation),
            onTap: { onOpenReproduction?() }
        )
    }

    private var careDueSection: some View {
        Group {
            if !careModel.items.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 6) {
                        Image(systemName: "bell.badge")
                            .font(.subheadline.weight(.semibold))
                        Text("Care Due")
                            .font(.subheadline.weight(.semibold))
                    }
                    .foregroundStyle(Theme.forestGreen)

                    ForEach(careDueStableRows, id: \.stableId) { pair in
                        let item = pair.item

                        HStack(spacing: 12) {
                            Image(systemName: item.overdue ? "exclamationmark.circle.fill" : "clock")
                                .foregroundStyle(item.overdue ? Theme.amber : Theme.forestGreen)
                                .frame(width: 20)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(Theme.textPrimary)
                                Text(item.typeLabel)
                                    .font(.caption)
                                    .foregroundStyle(Theme.textSecondary)
                            }
                            Spacer()
                            Text(dueText(for: item))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(dueColor(for: item))
                        }
                        .padding(.vertical, 4)
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("\(item.typeLabel): \(item.title), \(dueText(for: item))")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func dueText(for item: CareDueItem) -> String {
        let due = item.dueDate.friendlyString
        if item.overdue {
            return "Overdue — due \(due)"
        }
        let days = GestationCalculator.daysUntil(item.dueDate, today: todayKotlin)
        if days == 0 { return "Due today" }
        return GestationDueText.daysLabel(daysUntilDue: days)
    }

    private func dueColor(for item: CareDueItem) -> Color {
        if item.overdue { return .red }
        if GestationCalculator.daysUntil(item.dueDate, today: todayKotlin) <= 3 { return .orange }
        return Theme.amber
    }

    private var careDueStableRows: [(stableId: String, item: CareDueItem)] {
        var seen: [String: Int] = [:]
        return careModel.items.map { item in
            let base = item.careDueStableId
            let count = seen[base, default: 0]
            seen[base] = count + 1
            let stableId = count == 0 ? base : "\(base)-\(count)"
            return (stableId: stableId, item: item)
        }
    }

    func gestationDay(for gestation: Gestation_) -> Int {
        GestationCalculator.gestationDay(for: gestation, today: todayKotlin)
    }

    private func daysUntil(_ date: Kotlinx_datetimeLocalDate) -> Int {
        GestationCalculator.daysUntil(date, today: todayKotlin)
    }

    private var patientHeader: some View {
        VStack(spacing: 12) {
            Image(systemName: "pawprint.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(Theme.forestGreen)
            Text(patient.name)
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Theme.textPrimary)
            Text(patient.species)
                .font(.title3)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }

    private var basicInfoSection: some View {
        Section {
            infoRow(label: "Breed", value: patient.breed)
            infoRow(label: "Gender", value: patient.gender)
            infoRow(label: "Date of Birth", value: formatDate(patient.dateOfBirth))
            infoRow(label: "Location", value: patient.stableLocation)
        } header: {
            sectionHeader("Basic Information")
        }
    }

    private var ownerSection: some View {
        Section {
            if let ownerName = ownerName {
                HStack {
                    Image(systemName: "person.crop.circle.fill")
                        .foregroundStyle(Theme.forestGreen)
                    Text(ownerName)
                        .foregroundStyle(Theme.textPrimary)
                }
            }
        } header: {
            sectionHeader("Owner")
        }
    }

    private var identificationSection: some View {
        Section {
            infoRow(label: "Microchip", value: patient.microchipId)
            infoRow(label: "UELN", value: patient.ueln)
            infoRow(label: "Registration", value: patient.registrationNumber)
        } header: {
            sectionHeader("Identification")
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
    }

    private func infoRow(label: String, value: String?) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
            Text(value ?? "—")
                .foregroundStyle(value != nil ? Theme.textPrimary : Theme.textTertiary)
        }
        .font(.subheadline)
    }

    private func formatDate(_ date: Kotlinx_datetimeLocalDate?) -> String? {
        guard let date = date else { return nil }
        return date.displayString
    }
}

private extension CareDueItem {
    var careDueStableId: String { "\(typeLabel)-\(title)-\(dueDate.displayString)" }
}

@MainActor
final class CareDuePanelModel: ObservableObject {
    @Published var items: [CareDueItem] = []

    private var cancellable: NativeCancellable?
    private let store: UpcomingCareStore
    private var loadTask: Task<Void, Never>?

    init(patientId: Int64) {
        store = IosRecordStores.shared.upcomingCareStore(patientId: patientId)
        cancellable = store.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.items = state.items
            }
        })
        loadTask = Task { store.load() }
    }

    deinit {
        loadTask?.cancel()
        cancellable?.cancel()
    }
}

@MainActor
final class GestationPanelModel: ObservableObject {
    @Published var activeGestation: Gestation_?

    private var cancellable: NativeCancellable?
    private let store: GestationListStore
    private var loadTask: Task<Void, Never>?

    init(patientId: Int64) {
        store = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        cancellable = store.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.activeGestation = state.records.first { record in
                    RecordDetailOpener.shared.isGestationActive(gestation: record)
                }
            }
        })
        loadTask = Task { store.load() }
    }

    deinit {
        loadTask?.cancel()
        cancellable?.cancel()
    }
}
