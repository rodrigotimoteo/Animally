import SwiftUI
import Shared

struct PatientDetailView: View {
    @StateObject private var viewModel: PatientDetailViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab: DetailTab = .overview
    @State private var addRecordRoute: RecordEditRoute?
    @State private var selectedRecordKey: RecordDetailKey?
    /// Bumped when returning from a record editor so the visible tab's
    /// view model is recreated and reloads fresh data. Deduplicated via task.
    @State private var recordsRefreshToken = 0
    @State private var reloadTask: Task<Void, Never>?

    enum DetailTab: String, CaseIterable, Identifiable {
        case overview = "Overview"
        case medical = "Medical"
        case preventive = "Care"
        case reproduction = "Repro"
        case diagnostics = "Files"

        var id: String { rawValue }

        /// Full name for accessibility — the short segmented labels are
        /// compressed, VoiceOver should speak the real section names.
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
                    errorBanner(message: errorMessage)
                }
            }
            .onAppear {
                viewModel.load()
                debouncedReload()
            }
            .onChange(of: scenePhase) { _, phase in
                guard phase == .active else { return }
                viewModel.load()
                debouncedReload()
            }

            .onChange(of: addRecordRoute) { oldValue, newValue in
                // Returning from a record editor: reload visible tab once. Deduplicated
                // via debouncedReload so rapid nil→value→nil does not double-trigger.
                if oldValue != nil, newValue == nil {
                    debouncedReload()
                    viewModel.load()
                }
            }
            .navigationDestination(item: $addRecordRoute) { route in
                recordEditDestination(route)
            }
            .navigationDestination(item: $selectedRecordKey) { key in
                // Lazy detail via Kotlin opener — no eager field payload.
                RecordDetailView(displayType: key.displayType, patientId: key.patientId, recordId: key.recordId)
            }
    }

    private func debouncedReload() {
        reloadTask?.cancel()
        reloadTask = Task { @MainActor in
            // Small debounce avoids double reload when onAppear + scenePhase fire together
            try? await Task.sleep(nanoseconds: 20_000_000)
            guard !Task.isCancelled else { return }
            recordsRefreshToken += 1
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
                .id(recordsRefreshToken)
            case .medical:
                MedicalTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId in
                        selectedRecordKey = RecordDetailKey(displayType: type, patientId: patient.id, recordId: recordId)
                    }
                )
            case .preventive:
                PreventiveTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId in
                        selectedRecordKey = RecordDetailKey(displayType: type, patientId: patient.id, recordId: recordId)
                    }
                )
            case .reproduction:
                ReproductionTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId in
                        selectedRecordKey = RecordDetailKey(displayType: type, patientId: patient.id, recordId: recordId)
                    }
                )
            case .diagnostics:
                DiagnosticsTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId in
                        selectedRecordKey = RecordDetailKey(displayType: type, patientId: patient.id, recordId: recordId)
                    }
                )
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

struct OverviewTab: View {
    let patient: Patient_
    let ownerName: String?
    /// Switches the detail screen to the Reproduction tab.
    var onOpenReproduction: (() -> Void)? = nil

    @StateObject private var careModel: CareDuePanelModel
    @StateObject private var gestationModel: GestationPanelModel
    @State private var todayKotlin: Kotlinx_datetimeLocalDate
    @Environment(\.scenePhase) private var scenePhase

    private static func makeTodayKotlin() -> Kotlinx_datetimeLocalDate {
        let comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        return Kotlinx_datetimeLocalDate(
            year: Int32(comps.year ?? 1970),
            month: Int32(comps.month ?? 1),
            day: Int32(comps.day ?? 1)
        )
    }

    init(
        patient: Patient_,
        ownerName: String?,
        onOpenReproduction: (() -> Void)? = nil
    ) {
        self.patient = patient
        self.ownerName = ownerName
        self.onOpenReproduction = onOpenReproduction
        _todayKotlin = State(initialValue: Self.makeTodayKotlin())
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
        .onAppear { todayKotlin = Self.makeTodayKotlin() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                todayKotlin = Self.makeTodayKotlin()
            }
        }
    }

    /// Compact "In Foal" status card shown only while a gestation is active;
    /// taps through to the Reproduction tab. Uses cached todayKotlin for stable day calc.
    private func pregnancyCard(_ gestation: Gestation_) -> some View {
        let gestationDay = gestationDay(for: gestation)
        let due = gestation.expectedDueDate.friendlyString
        return Button {
            onOpenReproduction?()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "heart.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(Theme.forestGreen)

                VStack(alignment: .leading, spacing: 4) {
                    Text("In Foal")
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Theme.forestGreen)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())

                    Text("Day \(gestationDay) · Due \(due)")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.textPrimary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
            }
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("In foal, day \(gestationDay), due \(due). Opens reproduction tab")
    }

    /// Upcoming and overdue care, surfaced where the vet looks first.
    /// Static rows for v1; tapping through to the record is a follow-up.
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
        let due = "\(item.dueDate.dayOfMonth) \(Self.monthAbbreviation(item.dueDate.monthNumber)) \(item.dueDate.year)"
        if item.overdue {
            return "Overdue — due \(due)"
        }
        if daysUntil(item.dueDate) == 0 {
            return "Due today"
        }
        let days = daysUntil(item.dueDate)
        return "Due in \(days) day\(days == 1 ? "" : "s")"
    }

    private func dueColor(for item: CareDueItem) -> Color {
        if item.overdue { return .red }
        if daysUntil(item.dueDate) <= 3 { return .orange }
        return Theme.amber
    }

    /// Stable rows with deduplication counter — avoids global idx instability when list reorders.
    /// Duplicates get "-1", "-2" suffix; unique rows keep base id stable even when other items inserted.
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
        let todayEpoch = todayKotlin.epochDaysCompat()
        let breedingEpoch = gestation.breedingDate.epochDaysCompat()
        return max(0, Int(todayEpoch - breedingEpoch))
    }

    private func daysUntil(_ date: Kotlinx_datetimeLocalDate) -> Int {
        let todayEpoch = todayKotlin.epochDaysCompat()
        let dueEpoch = date.epochDaysCompat()
        return Int(dueEpoch - todayEpoch)
    }

    private static func monthAbbreviation(_ monthNumber: Int32) -> String {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let index = Int(monthNumber) - 1
        return index >= 0 && index < months.count ? months[index] : ""
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

// Stable identity for CareDueItem — Kotlin data class hash collapses duplicate title+type+date rows.
// Mirrors S3 Insights stableId pattern; uses "\(typeLabel)-\(title)-\(dueDate.displayString)" composite
// plus deduplication counter in careDueStableRows for uniqueness when two reminders share same fields.
private extension CareDueItem {
    var careDueStableId: String { "\(typeLabel)-\(title)-\(dueDate.displayString)" }
}

/// Swift-side observation for the Care Due panel: bridges the Kotlin
/// UpcomingCareStore's state flow into published properties. Subscription is
/// tied to view lifecycle — cancelled on deinit so rapid open/close cannot leak.
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

/// Observes the patient's gestation list so the Overview can surface an
/// active pregnancy. Single-source rule: active check mirrors Kotlin
/// CalculateGestationUseCase RESOLVED_STATUSES + isActive flag.
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

struct StubTabView: View {
    let title: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: systemImage)
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.4))
            Text("Coming soon")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("\(title) information will appear here")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
